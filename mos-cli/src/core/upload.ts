/**
 * 上传逻辑：分片 + 并发 + 秒传 + 断点续传（服务端返回已上传分片）。
 *
 * 分片大小策略与 Java SDK 的 IOUtils.splitFile 保持一致：
 *   partSize = ceil(totalSize / expectChunks)，再 clamp 到 [minPartSize, maxPartSize]
 */

import * as fs from 'node:fs';
import * as path from 'node:path';
import { open as fsOpen } from 'node:fs/promises';
import { MosClient } from './client.js';
import { basename, concurrentMap, md5Buffer, md5File, normalizePathname } from '../util/util.js';
import { info, ok, readableSize, readableSpeed, readableTime, warn } from '../util/ui.js';

export interface UploadOptions {
	/** 期望分片数，默认 50（与 Java SDK 一致） */
	expectChunks?: number;
	/** 最小分片大小，默认 5MB */
	minPartSize?: number;
	/** 最大分片大小，默认 20MB */
	maxPartSize?: number;
	/** 并发分片数，默认 5 */
	concurrency?: number;
	/** 覆盖同名文件 */
	cover?: boolean;
	/** 文件是否公开 */
	isPublic?: boolean;
	/** 指定存储路径，默认为本地文件名 */
	remotePath?: string;
	/** 手动指定 contentType */
	contentType?: string;
	/** 强制重新计算并上传（不做秒传） */
	force?: boolean;
	/** 进度回调 */
	onProgress?: (uploadedBytes: number, totalBytes: number) => void;
}

/**
 * 计算分片方案，与 Java IOUtils.splitFile 逻辑一致。
 */
export function planChunks(
	totalSize: number,
	expectChunks = 50,
	minPartSize = 5 * 1024 * 1024,
	maxPartSize = 20 * 1024 * 1024,
): { partSize: number; chunks: number } {
	// BigDecimal.valueOf(totalSize).divide(valueOf(expectChunks), 0, RoundingMode.UP)
	let partSize = Math.ceil(totalSize / Math.max(1, expectChunks));
	if (partSize < minPartSize) partSize = minPartSize;
	else if (partSize > maxPartSize) partSize = maxPartSize;

	const chunks = totalSize === 0 ? 1 : Math.ceil(totalSize / partSize);
	return { partSize, chunks };
}

const MIME_BY_EXT: Record<string, string> = {
	'.txt': 'text/plain',
	'.json': 'application/json',
	'.xml': 'application/xml',
	'.html': 'text/html',
	'.htm': 'text/html',
	'.css': 'text/css',
	'.js': 'application/javascript',
	'.ts': 'application/javascript',
	'.png': 'image/png',
	'.jpg': 'image/jpeg',
	'.jpeg': 'image/jpeg',
	'.gif': 'image/gif',
	'.webp': 'image/webp',
	'.svg': 'image/svg+xml',
	'.bmp': 'image/bmp',
	'.ico': 'image/x-icon',
	'.mp4': 'video/mp4',
	'.mov': 'video/quicktime',
	'.avi': 'video/x-msvideo',
	'.mp3': 'audio/mpeg',
	'.wav': 'audio/wav',
	'.pdf': 'application/pdf',
	'.zip': 'application/zip',
	'.gz': 'application/gzip',
	'.tar': 'application/x-tar',
	'.md': 'text/markdown',
	'.csv': 'text/csv',
};

export function guessContentType(filePath: string): string | undefined {
	const ext = path.extname(filePath).toLowerCase();
	return MIME_BY_EXT[ext];
}

export interface UploadResult {
	pathname: string;
	totalSize: number;
	chunks: number;
	instant: boolean;
	resumedChunks: number;
	costMs: number;
}

export async function uploadFile(client: MosClient, filePath: string, options: UploadOptions = {}): Promise<UploadResult> {
	const stat = await fs.promises.stat(filePath);
	if (!stat.isFile()) {
		throw new Error(`${filePath} 不是文件`);
	}

	const {
		expectChunks = 50,
		minPartSize = 5 * 1024 * 1024,
		maxPartSize = 20 * 1024 * 1024,
		concurrency = 5,
		cover: coverOpt = false,
		isPublic = false,
		remotePath,
		force = false,
		onProgress,
	} = options;

	// --force 的语义是「重新上传这个文件」，若目标已存在则必然要覆盖，
	// 否则服务端会在秒传之前就以「已存在相同的pathname」拒绝。
	const cover = coverOpt || force;

	const totalSize = stat.size;
	const pathname = normalizePathname(remotePath || basename(filePath));
	const contentType = options.contentType || guessContentType(filePath);
	const lastModified = Math.floor(stat.mtimeMs);

	const started = Date.now();

	// totalMd5 是整个文件的 MD5（服务端用它做秒传去重）
	info(`计算文件 MD5：${filePath}`);
	const totalMd5 = await md5File(filePath);

	const { partSize, chunks } = planChunks(totalSize, expectChunks, minPartSize, maxPartSize);

	// 1. 初始化
	const init = await client.initUpload({
		pathname,
		totalMd5,
		totalSize,
		chunks,
		cover,
		isPublic,
		lastModified,
		contentType,
	});

	if (init.fileExists) {
		if (!force) {
			ok(`秒传完成：${pathname}（${readableSize(totalSize)}），耗时 ${readableTime(Date.now() - started)}`);
			return {
				pathname,
				totalSize,
				chunks,
				instant: true,
				resumedChunks: chunks,
				costMs: Date.now() - started,
			};
		}
		// --force 也无法真正重传：服务端按 md5+size 去重，命中即直接复用
		// fileHouse，不会创建新的 UploadFile，此时继续传分片会报
		// 「uploadFile不存在」。因此这里退回秒传语义。
		warn('服务端按内容去重（md5+size），相同内容无法强制重传，已按秒传处理');
		warn('如需替换内容，请先删除远端文件：mos rm ' + pathname + ' --yes');
		return {
			pathname,
			totalSize,
			chunks,
			instant: true,
			resumedChunks: chunks,
			costMs: Date.now() - started,
		};
	}

	const uploaded = new Set<number>(init.existedChunkIndexs ?? []);
	if (force) {
		// --force 语义：不信任服务端已有的分片，全部重新上传
		uploaded.clear();
	} else if (uploaded.size > 0) {
		info(`服务端已存在 ${uploaded.size}/${chunks} 个分片，将跳过`);
	}

	// 2. 并发上传分片
	let uploadedBytes = 0;
	// 已存在的分片也算作进度
	for (const idx of uploaded) {
		const start = idx * partSize;
		uploadedBytes += Math.min(partSize, totalSize - start);
	}

	const fh = await fsOpen(filePath, 'r');
	try {
		const tasks = Array.from({ length: chunks }, (_, i) => i).filter((i) => !uploaded.has(i));

		await concurrentMap(tasks, concurrency, async (chunkIndex) => {
			const start = chunkIndex * partSize;
			const length = Math.min(partSize, totalSize - start);

			const buffer = Buffer.alloc(length);
			await fh.read(buffer, 0, length, start);

			const chunkMd5 = md5Buffer(buffer);
			await client.uploadChunk({
				pathname,
				totalMd5,
				totalSize,
				chunkMd5,
				chunkIndex,
				data: buffer,
			});

			uploadedBytes += length;
			onProgress?.(uploadedBytes, totalSize);
		});
	} finally {
		await fh.close();
	}

	// 3. 合并
	await client.mergeFiles({
		pathname,
		totalMd5,
		totalSize,
		chunks,
		isPublic,
		cover,
		updateMd5: false,
		wait: true,
		lastModified,
		contentType,
	});

	const costMs = Date.now() - started;
	ok(
		`上传完成：${pathname}（${readableSize(totalSize)}，${chunks} 个分片），耗时 ${readableTime(costMs)}，` +
			`平均 ${readableSpeed(totalSize / Math.max(costMs / 1000, 0.001))}`,
	);

	return { pathname, totalSize, chunks, instant: false, resumedChunks: uploaded.size, costMs };
}

/**
 * 上传目录（递归）。
 */
export async function uploadDir(
	client: MosClient,
	dirPath: string,
	remoteDir: string,
	options: UploadOptions = {},
): Promise<{ files: number; totalSize: number }> {
	const stat = await fs.promises.stat(dirPath);
	if (!stat.isDirectory()) {
		throw new Error(`${dirPath} 不是目录`);
	}

	const base = normalizePathname(remoteDir || `/${basename(dirPath)}`);
	let files = 0;
	let totalSize = 0;

	const walk = async (current: string): Promise<void> => {
		const entries = await fs.promises.readdir(current, { withFileTypes: true });
		entries.sort((a, b) => a.name.localeCompare(b.name));

		for (const entry of entries) {
			const full = path.join(current, entry.name);
			if (entry.isDirectory()) {
				await walk(full);
			} else if (entry.isFile()) {
				const rel = path.relative(dirPath, full).replace(/\\/g, '/');
				const remote = normalizePathname(`${base}/${rel}`);
				await uploadFile(client, full, { ...options, remotePath: remote });
				const st = await fs.promises.stat(full);
				totalSize += st.size;
				files++;
			}
		}
	};

	await walk(dirPath);
	ok(`目录上传完成：${files} 个文件，共 ${readableSize(totalSize)}`);
	return { files, totalSize };
}

/**
 * 上传 stdin 流（单分片），用于管道场景。
 */
export async function uploadStdin(client: MosClient, options: UploadOptions = {}): Promise<UploadResult> {
	const chunks: Buffer[] = [];
	for await (const chunk of process.stdin) {
		chunks.push(chunk as Buffer);
	}
	const data = Buffer.concat(chunks);
	const pathname = normalizePathname(options.remotePath || `stdin-${Date.now()}`);
	const totalSize = data.length;
	const started = Date.now();
	const totalMd5 = md5Buffer(data);

	const { chunks: partCount } = planChunks(totalSize, 1, 0, Number.MAX_SAFE_INTEGER);

	await client.initUpload({
		pathname,
		totalMd5,
		totalSize,
		chunks: partCount,
		cover: options.cover ?? false,
		isPublic: options.isPublic ?? false,
		lastModified: Date.now(),
		contentType: options.contentType,
	});

	if (partCount === 1) {
		await client.uploadChunk({
			pathname,
			totalMd5,
			totalSize,
			chunkMd5: totalMd5,
			chunkIndex: 0,
			data,
		});
	}

	await client.mergeFiles({
		pathname,
		totalMd5,
		totalSize,
		chunks: partCount,
		isPublic: options.isPublic ?? false,
		cover: options.cover ?? false,
		updateMd5: false,
		wait: true,
		lastModified: Date.now(),
		contentType: options.contentType,
	});

	ok(`stdin 上传完成：${pathname}（${readableSize(totalSize)}）`);
	return { pathname, totalSize, chunks: partCount, instant: false, resumedChunks: 0, costMs: Date.now() - started };
}
