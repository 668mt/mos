/**
 * 下载逻辑：写入临时文件 -> 原子替换，并还原 lastModified。
 * 与 Java SDK 的 MultipartOperation.downloadFile 行为一致。
 */

import * as fs from 'node:fs';
import * as path from 'node:path';
import { pipeline } from 'node:stream/promises';
import { Readable } from 'node:stream';
import { createWriteStream } from 'node:fs';
import { MosClient } from './client.js';
import { basename, expandHome, normalizePathname } from '../util/util.js';
import { isDirItem, joinItemPath, pageItems } from './dirTypes.js';
import { info, ok, readableSize, readableSpeed, readableTime } from '../util/ui.js';

export interface DownloadOptions {
	/** 本地保存路径（文件或目录） */
	destination?: string;
	/** 覆盖已存在的文件 */
	cover?: boolean;
	/** 限速，KB/s，0 或不传表示不限速 */
	limitSpeedKbSeconds?: number;
	/** 输出到 stdout 而不是文件 */
	toStdout?: boolean;
	/** 下载缩略图 */
	thumb?: boolean;
	onProgress?: (downloadedBytes: number, totalBytes: number) => void;
}

/** 限速流 */
function createLimitStream(source: NodeJS.ReadableStream, kbPerSecond: number): Readable {
	const bytesPerSecond = kbPerSecond * 1024;
	let sent = 0;
	const start = Date.now();

	async function* generate(): AsyncGenerator<Buffer> {
		for await (const chunk of source as AsyncIterable<Buffer | string>) {
			const buf = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
			let offset = 0;
			while (offset < buf.length) {
				const elapsed = (Date.now() - start) / 1000;
				const allowed = Math.max(bytesPerSecond * elapsed - sent, 0);
				if (allowed <= 0) {
					await new Promise((r) => setTimeout(r, 100));
					continue;
				}
				const size = Math.min(buf.length - offset, Math.floor(allowed));
				sent += size;
				yield buf.subarray(offset, offset + size);
				offset += size;
			}
		}
	}

	return Readable.from(generate());
}

/**
 * 决定最终的本地文件路径。
 */
export function resolveDestination(destination: string | undefined, pathname: string): string {
	const name = basename(pathname);
	if (!destination) {
		return path.resolve(process.cwd(), name === '/' ? 'download' : name);
	}
	const expanded = expandHome(destination);
	// 已存在的目录，或以分隔符结尾 -> 当作目录
	const isDirLike = /[\\/]$/.test(destination) || (fs.existsSync(expanded) && fs.statSync(expanded).isDirectory());
	if (isDirLike) {
		return path.resolve(expanded, name === '/' ? 'download' : name);
	}
	return path.resolve(expanded);
}

export interface DownloadResult {
	pathname: string;
	localPath: string;
	size: number;
	costMs: number;
}

export async function download(
	client: MosClient,
	remotePathname: string,
	options: DownloadOptions = {},
): Promise<DownloadResult> {
	const pathname = normalizePathname(remotePathname);
	const started = Date.now();

	const info_ = await client.getFileInfo(pathname);
	if (!info_) {
		throw new Error(`资源不存在：${pathname}`);
	}

	const totalSize = Number(info_.sizeByte ?? 0);
	const lastModified = Number(info_.lastModified ?? 0);

	// 输出到 stdout
	if (options.toStdout) {
		const res = await client.downloadStream(pathname);
		if (options.limitSpeedKbSeconds && options.limitSpeedKbSeconds > 0) {
			await pipeline(createLimitStream(res, options.limitSpeedKbSeconds), process.stdout);
		} else {
			await pipeline(res, process.stdout);
		}
		return { pathname, localPath: '<stdout>', size: totalSize, costMs: Date.now() - started };
	}

	const localPath = resolveDestination(options.destination, pathname);
	if (fs.existsSync(localPath) && !options.cover) {
		throw new Error(`本地文件已存在：${localPath}（使用 --cover 覆盖）`);
	}

	await fs.promises.mkdir(path.dirname(localPath), { recursive: true });

	const tmpPath = `${localPath}.mos-tmp`;
	const res = await client.downloadStream(pathname);

	let downloaded = 0;
	const counter = async function* (source: AsyncIterable<Buffer>) {
		for await (const chunk of source) {
			downloaded += chunk.length;
			options.onProgress?.(downloaded, totalSize);
			yield chunk;
		}
	};

	const body =
		options.limitSpeedKbSeconds && options.limitSpeedKbSeconds > 0
			? createLimitStream(res, options.limitSpeedKbSeconds)
			: res;

	try {
		await pipeline(counter(body), createWriteStream(tmpPath));
	} catch (e) {
		await fs.promises.rm(tmpPath, { force: true });
		throw e;
	}

	// 原子替换
	if (fs.existsSync(localPath)) {
		await fs.promises.rm(localPath, { force: true });
	}
	await fs.promises.rename(tmpPath, localPath);

	// 还原修改时间
	if (lastModified > 0) {
		try {
			await fs.promises.utimes(localPath, new Date(lastModified), new Date(lastModified));
		} catch {
			// 忽略 utimes 失败（例如文件系统不支持）
		}
	}

	const costMs = Date.now() - started;
	ok(
		`下载完成：${pathname} -> ${localPath}（${readableSize(totalSize)}），耗时 ${readableTime(costMs)}，` +
			`平均 ${readableSpeed(totalSize / Math.max(costMs / 1000, 0.001))}`,
	);

	return { pathname, localPath, size: totalSize, costMs };
}

/**
 * 下载整个目录（递归），依赖服务端 list 接口。
 */
export async function downloadDir(
	client: MosClient,
	remoteDir: string,
	localDir: string,
	options: DownloadOptions = {},
): Promise<{ files: number; totalSize: number }> {
	const base = normalizePathname(remoteDir);
	const root = expandHome(localDir || '.');
	let files = 0;
	let totalSize = 0;

	const walk = async (remotePath: string): Promise<void> => {
		// 目录路径统一去掉结尾的 /，避免 list 的前缀匹配出错
		const dirPath = normalizePathname(remotePath).replace(/\/+$/, '') || '/';
		const page = await client.list(dirPath, { pageSize: 1000 });
		for (const item of pageItems(page)) {
			const child = joinItemPath(dirPath, item);
			if (!child || child === '/') continue;
			if (isDirItem(item)) {
				await walk(child);
			} else {
				const rel = child.startsWith(base) ? child.slice(base.length) : child;
				const dest = path.join(root, rel);
				const res = await download(client, child, { ...options, destination: dest, cover: true });
				files++;
				totalSize += res.size;
			}
		}
	};

	info(`开始下载目录：${base} -> ${root}`);
	await walk(base);
	ok(`目录下载完成：${files} 个文件，共 ${readableSize(totalSize)}`);
	return { files, totalSize };
}
