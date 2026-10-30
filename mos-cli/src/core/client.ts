/**
 * MOS 开放接口 HTTP 客户端。
 *
 * 服务端统一返回信封格式：
 *   { "status": "ok", "message": null, "result": ... }
 * 失败时 status != "ok"，并通过 message 说明原因。
 */

import * as http from 'node:http';
import * as https from 'node:https';
import { URL } from 'node:url';
import { Readable } from 'node:stream';
import { ResolvedConfig } from './config.js';
import { DEFAULT_CHECK_URI, externHostOrDefault, isHostAvailable, ParsedHost, pickHost } from './hosts.js';
import { encrypt, PathnamesEncryptContent } from './sign.js';
import { encodePathname, normalizePathname } from '../util/util.js';

export class MosError extends Error {
	readonly status: string | undefined;
	readonly httpStatus: number | undefined;

	constructor(message: string, status?: string, httpStatus?: number) {
		super(message);
		this.name = 'MosError';
		this.status = status;
		this.httpStatus = httpStatus;
	}
}

export interface ResResult<T = unknown> {
	status: string;
	message?: string;
	result: T;
}

interface RequestOptions {
	method?: string;
	headers?: Record<string, string>;
	body?: Buffer | string;
	timeout?: number;
	/** 跟随重定向，下载场景默认开启 */
	redirect?: boolean;
}

const DEFAULT_TIMEOUT = 120_000;

/** 底层请求，返回原始响应 */
function rawRequest(url: string, options: RequestOptions = {}): Promise<http.IncomingMessage> {
	return new Promise((resolve, reject) => {
		let parsed: URL;
		try {
			parsed = new URL(url);
		} catch {
			return reject(new MosError(`无效的 URL：${url}`));
		}

		const isHttps = parsed.protocol === 'https:';
		const lib = isHttps ? https : http;

		const req = lib.request(
			parsed,
			{
				method: options.method ?? 'GET',
				headers: options.headers ?? {},
				// MOS 服务端常在自签名/内网证书后面，这里与 Java SDK 保持一致（不校验证书）
				rejectUnauthorized: false,
			},
			(res) => {
				resolve(res);
			},
		);

		req.setTimeout(options.timeout ?? DEFAULT_TIMEOUT, () => {
			req.destroy(new MosError(`请求超时：${url}`));
		});

		req.on('error', (e) => {
			reject(
				e instanceof MosError
					? e
					: new MosError(`请求失败：${(e as Error).message}（${options.method ?? 'GET'} ${url}）`),
			);
		});

		if (options.body !== undefined) {
			req.write(options.body);
		}
		req.end();
	});
}

async function readBody(res: http.IncomingMessage): Promise<Buffer> {
	const chunks: Buffer[] = [];
	for await (const chunk of res) {
		chunks.push(chunk as Buffer);
	}
	return Buffer.concat(chunks);
}

/**
 * 发送请求并解析 ResResult 信封。
 */
export async function request<T>(
	url: string,
	options: RequestOptions = {},
): Promise<ResResult<T>> {
	const res = await rawRequest(url, options);
	const body = await readBody(res);
	const text = body.toString('utf8');

	if (!text) {
		if (res.statusCode && res.statusCode >= 400) {
			throw new MosError(`HTTP ${res.statusCode}：${url}`, undefined, res.statusCode);
		}
		return { status: 'ok', result: undefined as T };
	}

	let parsed: ResResult<T>;
	try {
		parsed = JSON.parse(text) as ResResult<T>;
	} catch {
		// 非 JSON 响应（例如服务端异常页）
		throw new MosError(
			`服务端返回了非 JSON 内容（HTTP ${res.statusCode}）：${text.slice(0, 200)}`,
			undefined,
			res.statusCode,
		);
	}

	if (String(parsed.status).toLowerCase() !== 'ok') {
		throw new MosError(parsed.message || '请求失败', parsed.status, res.statusCode);
	}

	return parsed;
}

/** 构造 multipart/form-data 请求体（不落盘，流式） */
export function buildMultipart(
	parts: Array<{ name: string; value: string | Buffer; filename?: string }>,
	boundary: string,
): { body: Buffer; contentType: string } {
	const chunks: Buffer[] = [];
	for (const part of parts) {
		let head = `--${boundary}\r\nContent-Disposition: form-data; name="${part.name}"`;
		if (part.filename !== undefined) {
			head += `; filename="${part.filename}"`;
		}
		head += '\r\n';
		head += 'Content-Type: application/octet-stream\r\n\r\n';
		chunks.push(Buffer.from(head, 'utf8'));
		chunks.push(typeof part.value === 'string' ? Buffer.from(part.value, 'utf8') : part.value);
		chunks.push(Buffer.from('\r\n', 'utf8'));
	}
	chunks.push(Buffer.from(`--${boundary}--\r\n`, 'utf8'));

	return {
		body: Buffer.concat(chunks),
		contentType: `multipart/form-data; boundary=${boundary}`,
	};
}

let boundaryCounter = 0;
function nextBoundary(): string {
	boundaryCounter += 1;
	return `----MosCliBoundary${Date.now().toString(36)}${boundaryCounter}`;
}

/** 报错信息里去掉冗长的完整 URL，只保留原因 */
function stripUrl(message: string): string {
	return message.replace(/（(?:GET|POST|PUT|DELETE)\s+\S+）/g, '');
}

/**
 * 规范化 list 的 keyWord。
 *
 * 服务端会按空白拆分并解析 f:/p:/e: 前缀，裸关键字语义不清，
 * 这里默认补 f: 表示「按文件名匹配」。
 */
function normalizeKeyWord(keyWord?: string): string | undefined {
	if (!keyWord) return undefined;
	const trimmed = keyWord.trim();
	if (!trimmed) return undefined;
	// 已带前缀的原样透传
	if (/^(f:|p:|e:|ef:|ep:)/.test(trimmed)) return trimmed;
	return `f:${trimmed}`;
}

/**
 * MOS 客户端，封装开放接口的签名与请求细节。
 */
export class MosClient {
	readonly config: ResolvedConfig;
	private readonly hosts: ParsedHost[];
	/** 缓存上次成功的地址，优先复用 */
	private preferredHost: string | null = null;

	constructor(config: ResolvedConfig) {
		this.config = config;
		this.hosts = config.hosts ?? [];
	}

	/**
	 * 在所有地址中挑选一个可用的。
	 *
	 * - 优先复用上次成功的地址（并做一次健康检查）
	 * - 否则按权重随机选择
	 */
	async pickHost(checkUri: string | null = DEFAULT_CHECK_URI): Promise<string> {
		if (this.hosts.length === 0) {
			throw new Error('未配置 host，请执行：mos config set --host <地址>');
		}
		if (this.hosts.length === 1) {
			return this.hosts[0].host;
		}

		if (this.preferredHost) {
			const hit = this.hosts.find((h) => h.host === this.preferredHost);
			if (hit && (!checkUri || (await isHostAvailable(hit.host, checkUri)))) {
				return hit.host;
			}
			this.preferredHost = null;
		}

		const picked = await pickHost(this.hosts, { checkUri });
		if (!picked) {
			throw new Error(
				`所有地址都不可用：${this.hosts.map((h) => h.host).join(', ')}\n` +
					'请检查网络，或用 mos config show 查看当前配置。',
			);
		}
		return picked.host;
	}

	/** 依次尝试各地址，直到成功（用于上传/下载等必须成功的操作） */
	private async withFailover<T>(fn: (host: string) => Promise<T>): Promise<T> {
		if (this.hosts.length === 0) {
			throw new Error('未配置 host，请执行：mos config set --host <地址>');
		}
		if (this.hosts.length === 1) {
			const result = await fn(this.hosts[0].host);
			this.preferredHost = this.hosts[0].host;
			return result;
		}

		// 先试上次成功的地址，再按权重回退
		const ordered = [...this.hosts].sort((a, b) => {
			if (a.host === this.preferredHost) return -1;
			if (b.host === this.preferredHost) return 1;
			return b.weight - a.weight;
		});

		const errors: string[] = [];
		for (const h of ordered) {
			try {
				const result = await fn(h.host);
				this.preferredHost = h.host;
				return result;
			} catch (e) {
				// 签名/业务错误重试没有意义，直接抛出
				if (e instanceof MosError && e.httpStatus !== undefined && e.httpStatus < 500) {
					throw e;
				}
				errors.push(`${h.host}: ${stripUrl((e as Error).message)}`);
			}
		}
		throw new Error(`所有地址都失败：\n  ${errors.join('\n  ')}`);
	}

	/** 当前选中的地址（用于日志） */
	get host(): string {
		return this.preferredHost ?? this.hosts[0]?.host ?? '';
	}

	/**
	 * 当前选中的对外地址：优先用配置里的外网地址（[] 中的），没配时回退到内网地址。
	 * 拼给最终用户（外网）的链接用它。
	 */
	get externHost(): string {
		const current = this.hosts.find((h) => h.host === this.preferredHost) ?? this.hosts[0];
		return current ? externHostOrDefault(current) : '';
	}

	/**
	 * 生成签名。expireSeconds 为 -1 表示永不过期。
	 */
	getSign(pathname: string, expireSeconds: number): string {
		const normalized = normalizePathname(pathname);
		return encrypt(
			this.config.secretKey,
			new PathnamesEncryptContent(normalized),
			this.config.bucket,
			this.config.openId,
			expireSeconds,
		);
	}

	/** 生成 sign + pathname 查询串，与 SDK 的 getSignQueryParams 一致 */
	private signQuery(pathname: string, expireSeconds = 600): string {
		const sign = this.getSign(pathname, expireSeconds);
		return `sign=${encodeURIComponent(sign)}&pathname=${encodeURIComponent(normalizePathname(pathname))}`;
	}

	/** 上传/下载签名时长：与 Java SDK 的 2 小时保持一致 */
	private static readonly UPLOAD_SIGN_EXPIRE = 2 * 60 * 60;

	// ---------------------------------------------------------------- 上传

	/**
	 * 上传初始化。命中秒传时返回 fileExists=true。
	 */
	async initUpload(params: {
		pathname: string;
		totalMd5: string;
		totalSize: number;
		chunks: number;
		cover: boolean;
		isPublic: boolean;
		lastModified: number;
		contentType?: string;
	}): Promise<{ fileExists: boolean; existedChunkIndexs?: number[] }> {
		const pathname = normalizePathname(params.pathname);
		const sign = this.getSign(pathname, MosClient.UPLOAD_SIGN_EXPIRE);

		const parts: Array<{ name: string; value: string | Buffer; filename?: string }> = [
			{ name: 'pathname', value: pathname },
			{ name: 'totalMd5', value: params.totalMd5 },
			{ name: 'totalSize', value: String(params.totalSize) },
			{ name: 'chunks', value: String(params.chunks) },
			{ name: 'cover', value: String(params.cover) },
			{ name: 'isPublic', value: String(params.isPublic) },
			{ name: 'lastModified', value: String(params.lastModified) },
		];
		if (params.contentType) {
			parts.push({ name: 'contentType', value: params.contentType });
		}

		const boundary = nextBoundary();
		const { body, contentType } = buildMultipart(parts, boundary);

		return this.withFailover(async (host) => {
			// 服务端 initUpload 用 @RequestParam 读取 cover / isPublic，
			// 因此必须放在查询参数里；放在表单中不会被生效。
			const query = new URLSearchParams({
				sign,
				cover: String(params.cover),
				isPublic: String(params.isPublic),
			});
			const url = `${host}/upload/${encodeURIComponent(this.config.bucket)}/init?${query}`;
			const res = await request<{ fileExists: boolean; existedChunkIndexs?: number[] }>(url, {
				method: 'POST',
				headers: { 'Content-Type': contentType, 'Content-Length': String(body.length) },
				body,
				timeout: 300_000,
			});
			return res.result;
		});
	}

	/**
	 * 上传单个分片。
	 */
	async uploadChunk(params: {
		pathname: string;
		totalMd5: string;
		totalSize: number;
		chunkMd5: string;
		chunkIndex: number;
		data: Buffer;
	}): Promise<void> {
		const pathname = normalizePathname(params.pathname);
		const sign = this.getSign(pathname, MosClient.UPLOAD_SIGN_EXPIRE);

		const parts: Array<{ name: string; value: string | Buffer; filename?: string }> = [
			{ name: 'file', value: params.data, filename: 'file' },
			{ name: 'pathname', value: pathname },
			{ name: 'totalMd5', value: params.totalMd5 },
			{ name: 'totalSize', value: String(params.totalSize) },
			{ name: 'chunkMd5', value: params.chunkMd5 },
			{ name: 'chunkIndex', value: String(params.chunkIndex) },
		];

		const boundary = nextBoundary();
		const { body, contentType } = buildMultipart(parts, boundary);

		await this.withFailover(async (host) => {
			const url = `${host}/upload/${encodeURIComponent(this.config.bucket)}?sign=${encodeURIComponent(sign)}`;
			await request(url, {
				method: 'POST',
				headers: { 'Content-Type': contentType, 'Content-Length': String(body.length) },
				body,
				timeout: 600_000,
			});
		});
	}

	/**
	 * 合并分片。
	 */
	async mergeFiles(params: {
		pathname: string;
		totalMd5: string;
		totalSize: number;
		chunks: number;
		isPublic: boolean;
		cover: boolean;
		updateMd5: boolean;
		wait: boolean;
		lastModified: number;
		contentType?: string;
	}): Promise<void> {
		const pathname = normalizePathname(params.pathname);
		const sign = this.getSign(pathname, MosClient.UPLOAD_SIGN_EXPIRE);

		const parts: Array<{ name: string; value: string | Buffer; filename?: string }> = [
			{ name: 'totalMd5', value: params.totalMd5 },
			{ name: 'totalSize', value: String(params.totalSize) },
			{ name: 'chunks', value: String(params.chunks) },
			{ name: 'isPublic', value: String(params.isPublic) },
			{ name: 'cover', value: String(params.cover) },
			{ name: 'updateMd5', value: String(params.updateMd5) },
			{ name: 'wait', value: String(params.wait) },
			{ name: 'pathname', value: pathname },
			{ name: 'lastModified', value: String(params.lastModified) },
		];
		if (params.contentType) {
			parts.push({ name: 'contentType', value: params.contentType });
		}

		const boundary = nextBoundary();
		const { body, contentType } = buildMultipart(parts, boundary);

		await this.withFailover(async (host) => {
			// 同 init：这些字段服务端用 @RequestParam 读取，必须放查询参数
			const query = new URLSearchParams({
				bucketName: this.config.bucket,
				sign,
				isPublic: String(params.isPublic),
				cover: String(params.cover),
				updateMd5: String(params.updateMd5),
				wait: String(params.wait),
			});
			const url = `${host}/upload/mergeFiles?${query}`;
			await request(url, {
				method: 'POST',
				headers: { 'Content-Type': contentType, 'Content-Length': String(body.length) },
				body,
				timeout: 600_000,
			});
		});
	}

	// ---------------------------------------------------------------- 下载

	/** 资源信息 */
	async getFileInfo(pathname: string): Promise<Record<string, any> | null> {
		const exists = await this.isExists(pathname);
		if (!exists) return null;
		return this.withFailover(async (host) => {
			const url = `${host}/open/resource/${encodeURIComponent(this.config.bucket)}/info?${this.signQuery(pathname)}`;
			const res = await request<Record<string, any>>(url);
			return res.result;
		});
	}

	/** 判断文件是否存在 */
	async isExists(pathname: string): Promise<boolean> {
		return this.withFailover(async (host) => {
			const url = `${host}/open/resource/${encodeURIComponent(this.config.bucket)}/isExists?${this.signQuery(pathname)}`;
			const res = await request<boolean>(url);
			return res.result === true;
		});
	}

	/** 构造资源访问 URL（对应 OpenMosController 的 /mos/{bucketName}/**） */
	private buildResourceUrl(
		host: string,
		pathname: string,
		opts: { expireSeconds?: number; thumb?: boolean; render?: boolean } = {},
	): string {
		const expireSeconds = opts.expireSeconds ?? MosClient.UPLOAD_SIGN_EXPIRE;
		const sign = this.getSign(pathname, expireSeconds);
		let url = `${host}/mos/${encodeURIComponent(this.config.bucket)}${encodePathname(pathname)}?sign=${encodeURIComponent(sign)}`;
		if (opts.thumb) url += '&thumb=true';
		if (opts.render) url += '&render=true';
		return url;
	}

	/**
	 * 生成资源访问链接（同步，供展示用）。
	 * 面向最终用户，使用对外地址（配置里的外网地址，没配则用内网地址）。
	 */
	getResourceUrl(pathname: string, opts: { expireSeconds?: number; thumb?: boolean; render?: boolean } = {}): string {
		return this.buildResourceUrl(this.externHost, pathname, opts);
	}
	/**
	 * 下载资源，直接返回响应流（调用方负责消费与关闭）。
	 */
	async downloadStream(pathname: string): Promise<http.IncomingMessage> {
		const opts = { expireSeconds: MosClient.UPLOAD_SIGN_EXPIRE };

		return this.withFailover(async (host) => {
			const url = this.buildResourceUrl(host, pathname, opts);
			let res = await rawRequest(url, { method: 'GET', redirect: true, timeout: 600_000 });

			// 跟随重定向（Java SDK 关闭了自动重定向，这里手动处理）
			let redirects = 0;
			while (res.statusCode && res.statusCode >= 300 && res.statusCode < 400 && res.headers.location && redirects < 5) {
				res.resume();
				const next = new URL(res.headers.location, url).toString();
				res = await rawRequest(next, { method: 'GET', redirect: true, timeout: 600_000 });
				redirects++;
			}

			if (res.statusCode && res.statusCode >= 400) {
				const body = await readBody(res);
				res.resume();
				throw new MosError(
					`下载失败，HTTP ${res.statusCode}：${body.toString('utf8').slice(0, 200) || pathname}`,
					undefined,
					res.statusCode,
				);
			}

			return res;
		});
	}

	/** 下载到 Buffer（用于小文件或 --stdout） */
	async downloadBuffer(pathname: string): Promise<Buffer> {
		const res = await this.downloadStream(pathname);
		return readBody(res);
	}

	// ---------------------------------------------------------------- 查询

	/**
	 * 查询文件列表
	 *
	 * keyWord 说明：服务端把它当作搜索语法解析，支持
	 *   f:关键字  按文件名匹配（默认）
	 *   p:关键字  按路径匹配
	 *   e:关键字  排除匹配
	 *   ef:关键字 路径包含且文件名排除
	 * 直接传裸关键字时，这里补上 f: 前缀以匹配文件名。
	 */
	async list(
		pathname: string,
		opts: { keyWord?: string; pageNum?: number; pageSize?: number } = {},
	): Promise<Record<string, any>> {
		const path = normalizePathname(pathname);
		const sign = this.getSign(path, 30);
		const keyWord = normalizeKeyWord(opts.keyWord);
		return this.withFailover(async (host) => {
			let url =
				`${host}/open/resource/${encodeURIComponent(this.config.bucket)}/list` +
				`?sign=${encodeURIComponent(sign)}&path=${encodeURIComponent(path)}`;
			if (keyWord) url += `&keyWord=${encodeURIComponent(keyWord)}`;
			if (opts.pageNum) url += `&pageNum=${opts.pageNum}`;
			if (opts.pageSize) url += `&pageSize=${opts.pageSize}`;
			const res = await request<Record<string, any>>(url);
			return res.result;
		});
	}

	// ---------------------------------------------------------------- 删除

	async deleteFile(pathname: string): Promise<boolean> {
		return this.withFailover(async (host) => {
			const url = `${host}/open/resource/${encodeURIComponent(this.config.bucket)}/deleteFile?${this.signQuery(pathname)}`;
			const res = await request<boolean>(url, { method: 'DELETE' });
			return res.result === true;
		});
	}

	async deleteDir(pathname: string): Promise<boolean> {
		return this.withFailover(async (host) => {
			const url = `${host}/open/dir/${encodeURIComponent(this.config.bucket)}/deleteDir?${this.signQuery(pathname)}`;
			const res = await request<boolean>(url, { method: 'DELETE' });
			return res.result === true;
		});
	}

	/** 重命名 */
	async rename(pathname: string, desPathname: string): Promise<void> {
		const sign = this.getSign(normalizePathname(pathname), 600);
		await this.withFailover(async (host) => {
			const url =
				`${host}/open/resource/${encodeURIComponent(this.config.bucket)}/rename` +
				`?sign=${encodeURIComponent(sign)}` +
				`&pathname=${encodeURIComponent(normalizePathname(pathname))}` +
				`&desPathname=${encodeURIComponent(normalizePathname(desPathname))}`;
			await request(url, { method: 'PUT' });
		});
	}

	// ---------------------------------------------------------------- 短链

	/** 创建短链接（对应 POST /s） */
	async createShortUrl(
		pathname: string,
		opts: { expireSeconds: number; queryString?: string },
	): Promise<string> {
		const sign = this.getSign(pathname, opts.expireSeconds);
		const body: Record<string, unknown> = { expireSeconds: opts.expireSeconds };
		if (opts.queryString) body.queryString = opts.queryString;
		const payload = Buffer.from(JSON.stringify(body), 'utf8');

		return this.withFailover(async (host) => {
			const url =
				`${host}/s?bucketName=${encodeURIComponent(this.config.bucket)}` +
				`&pathname=${encodeURIComponent(normalizePathname(pathname))}` +
				`&sign=${encodeURIComponent(sign)}`;
			const res = await request<{ shortCode: string }>(url, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json', 'Content-Length': String(payload.length) },
				body: payload,
			});
			return res.result.shortCode;
		});
	}
}

/** 判断响应对象是否为可读的字节流 */
export function isStream(res: unknown): res is Readable {
	return (
		typeof res === 'object' &&
		res !== null &&
		typeof (res as Readable).pipe === 'function' &&
		typeof (res as Readable).on === 'function'
	);
}
