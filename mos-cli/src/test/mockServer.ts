/**
 * 一个最小化的 MOS 服务端实现，用于验证 mos-cli 的行为。
 *
 * 它复刻了 OpenUploadController / OpenResourceController / OpenDirController
 * 以及 OpenApiAspect + AccessControlService 的核心契约：
 *   - ResResult 信封：{ status, message, result }
 *   - sign 参数校验（默认使用 Java SDK 生成的签名，由 verify-sign 校验）
 *   - init -> chunk -> mergeFiles 的分片上传流程
 *   - /mos/{bucket}/** 资源下载
 *
 * 注意：签名校验逻辑与 Java SDK 共用同一份密钥派生实现，
 * 详见 src/test/verify-sign.ts 中通过 JVM 调用真实 Java 代码的交叉验证测试。
 */

import * as http from 'node:http';
import * as crypto from 'node:crypto';
import { URL } from 'node:url';
import { decrypt } from '../core/sign.js';

interface Chunk {
	data: Buffer;
	chunkMd5: string;
}

interface FileHouse {
	md5: string;
	size: number;
	chunks: Chunk[];
	merged: boolean;
}

interface Resource {
	pathname: string;
	fileHouseId: string;
	sizeByte: number;
	lastModified: number;
	contentType?: string;
	isPublic: boolean;
}

export interface MockServerOptions {
	secretKey: string;
	openId?: number;
	bucketName?: string;
	/** 关闭签名校验，便于单独调试其它逻辑 */
	skipSignCheck?: boolean;
}

export class MockMosServer {
	private server: http.Server;
	private fileHouses = new Map<string, FileHouse>();
	private resources = new Map<string, Resource>();
	private pending = new Map<string, { totalMd5: string; totalSize: number; chunks: number; pathname: string }>();
	private counters = { init: 0, chunk: 0, merge: 0, download: 0, list: 0, delete: 0, info: 0 };

	readonly opts: Required<MockServerOptions>;

	constructor(options: MockServerOptions) {
		this.opts = {
			secretKey: options.secretKey,
			openId: options.openId ?? 0,
			bucketName: options.bucketName ?? 'default',
			skipSignCheck: options.skipSignCheck ?? false,
		};

		this.server = http.createServer((req, res) => {
			void this.handle(req, res).catch((e) => {
				this.fail(res, 500, String(e));
			});
		});
	}

	get counters_(): Readonly<typeof this.counters> {
		return this.counters;
	}

	/** 读取某个资源的内容（供测试断言） */
	getContent(pathname: string): Buffer | null {
		const res = this.resources.get(pathname);
		if (!res) return null;
		const fh = this.fileHouses.get(res.fileHouseId);
		if (!fh || !fh.merged) return null;
		return Buffer.concat(fh.chunks.map((c) => c.data));
	}

	async listen(port = 0): Promise<number> {
		await new Promise<void>((resolve) => this.server.listen(port, '127.0.0.1', resolve));
		const addr = this.server.address();
		if (addr === null || typeof addr === 'string') throw new Error('无法获取端口');
		return addr.port;
	}

	async close(): Promise<void> {
		await new Promise<void>((resolve) => this.server.close(() => resolve()));
	}

	// ------------------------------------------------------------ 路由

	private async handle(req: http.IncomingMessage, res: http.ServerResponse): Promise<void> {
		const url = new URL(req.url ?? '/', 'http://localhost');
		// 服务端用 Java 的 URLDecoder.decode 还原 pathname：
		// 既要解 %XX，也要把 + 还原成空格
		const p = javaUrlDecode(url.pathname);

		if (p === '/__counters') {
			return this.json(res, 200, { status: 'ok', result: this.counters });
		}

		// 健康检查端点，对应 Java SDK HostChooseUtils 使用的 /index.html
		if (p === '/index.html') {
			res.writeHead(200, { 'Content-Type': 'text/html;charset=utf-8' });
			res.end('ok');
			return;
		}

		if (p.startsWith('/upload/') && p.endsWith('/init')) {
			const bucketName = p.split('/')[2];
			return this.handleInit(req, res, url, bucketName);
		}

		if (p === '/upload/mergeFiles') {
			return this.handleMerge(req, res, url);
		}

		if (p.startsWith('/upload/')) {
			const bucketName = p.split('/')[2];
			return this.handleChunk(req, res, url, bucketName);
		}

		if (p.startsWith('/open/resource/')) {
			// /open/resource/{bucket}/{action}
			const segs = p.split('/').filter((s) => s !== '');
			const bucketName = segs[2] ?? '';
			const action = segs[3] ?? '';
			if (action === 'isExists') return this.handleIsExists(res, url, bucketName);
			if (action === 'info') return this.handleInfo(res, url, bucketName);
			if (action === 'list') return this.handleList(res, url, bucketName);
			if (action === 'deleteFile') return this.handleDeleteFile(req, res, url, bucketName);
			if (action === 'rename') return this.handleRename(req, res, url, bucketName);
		}

		if (p.startsWith('/open/dir/')) {
			const segs = p.split('/').filter((s) => s !== '');
			if (segs[3] === 'deleteDir') return this.handleDeleteDir(req, res, url, segs[2] ?? '');
		}

		if (p.startsWith('/mos/')) {
			return this.handleDownload(req, res, url, p);
		}

		// 短链创建（对应 OpenMosController#createShortUrl）
		if (p === '/s' && req.method === 'POST') {
			return this.handleCreateShortUrl(req, res, url);
		}

		// 短链解析跳转（对应 OpenMosController#resolveShortUrl）
		if (p.startsWith('/s/') && req.method === 'GET') {
			return this.handleResolveShortUrl(res, p);
		}

		return this.fail(res, 404, `未实现的接口：${p}`);
	}

	// ------------------------------------------------------------ 短链

	private shortCodes = new Map<string, { pathname: string; sign: string; queryString?: string }>();

	private async handleCreateShortUrl(
		req: http.IncomingMessage,
		res: http.ServerResponse,
		url: URL,
	): Promise<void> {
		const bucketName = url.searchParams.get('bucketName') ?? '';
		const pathname = url.searchParams.get('pathname') ?? '';
		const sign = url.searchParams.get('sign') ?? '';

		// 与服务端一致的校验
		if (!bucketName) return this.fail(res, 400, 'bucketName不能为空');
		if (!pathname) return this.fail(res, 400, 'pathname不能为空');
		if (!sign) return this.fail(res, 400, 'sign不能为空');
		if (!pathname.startsWith('/')) return this.fail(res, 400, 'pathname必须以/开头');
		if (sign.includes('|')) return this.fail(res, 400, 'sign不能包含|');

		let body: { expireSeconds?: number; queryString?: string } = {};
		try {
			const chunks: Buffer[] = [];
			for await (const c of req) chunks.push(c as Buffer);
			body = JSON.parse(Buffer.concat(chunks).toString('utf8') || '{}');
		} catch {
			// 忽略解析失败
		}
		const expireSeconds = Number(body.expireSeconds ?? 0);
		if (!Number.isFinite(expireSeconds) || expireSeconds <= 0) {
			return this.fail(res, 400, 'expireSeconds必须为正整数');
		}
		if (expireSeconds < 60) {
			return this.fail(res, 400, 'expireSeconds不能小于60秒');
		}

		// 校验签名
		try {
			this.checkSign(url, pathname);
		} catch (e) {
			return this.fail(res, 403, (e as Error).message);
		}

		// 生成 6 位短码
		let code = '';
		const alphabet = 'abcdefghijklmnopqrstuvwxyz0123456789';
		do {
			code = Array.from({ length: 6 }, () => alphabet[Math.floor(Math.random() * alphabet.length)]).join('');
		} while (this.shortCodes.has(code));

		this.shortCodes.set(code, { pathname, sign, queryString: body.queryString });
		return this.json(res, 200, { status: 'ok', result: { shortCode: code } });
	}

	// ------------------------------------------------------------ 短链解析

	private handleResolveShortUrl(res: http.ServerResponse, decodedPath: string): void {
		this.counters.download++;
		const code = decodedPath.slice('/s/'.length);
		const entry = this.shortCodes.get(code);
		if (!entry) {
			res.writeHead(404, { 'Content-Type': 'application/json;charset=utf-8' });
			res.end(JSON.stringify({ status: 'error', message: '短链接不存在', result: null }));
			return;
		}
		let location = `/mos/${this.opts.bucketName}${entry.pathname}?sign=${entry.sign}`;
		if (entry.queryString) {
			location += `&${entry.queryString}`;
		}
		res.writeHead(302, { Location: location, 'Access-Control-Allow-Origin': '*' });
		res.end();
	}

	// ------------------------------------------------------------ 签名

	/**
	 * 校验 sign 参数。使用与服务端相同的密钥派生与签名格式解析。
	 */
	private checkSign(url: URL, pathname: string | null): void {
		if (this.opts.skipSignCheck) return;

		const sign = url.searchParams.get('sign');
		if (!sign) {
			throw new HttpError(403, '没有权限访问：缺少 sign');
		}

		let decoded;
		try {
			decoded = decrypt(this.opts.secretKey, sign);
		} catch {
			throw new HttpError(403, '没有权限访问：签名验证失败');
		}

		if (decoded.bucketName !== this.opts.bucketName) {
			throw new HttpError(403, `bucket校验错误：${decoded.bucketName}`);
		}

		// 签名过期检查
		if (decoded.expireSeconds > 0) {
			if (Date.now() > decoded.signTime + decoded.expireSeconds * 1000) {
				throw new HttpError(403, '签名已过期');
			}
		}

		// pathname 必须包含在签名内容中（对应 PathnameSignChecker）
		if (pathname !== null) {
			const normalized = pathname.startsWith('/') ? pathname : '/' + pathname;
			const allowed = decoded.pathnames.map((s) => (s.startsWith('/') ? s : '/' + s));
			if (!allowed.includes(normalized)) {
				throw new HttpError(403, `签名不包含该路径：${normalized}`);
			}
		}
	}

	// ------------------------------------------------------------ 上传

	private async handleInit(
		req: http.IncomingMessage,
		res: http.ServerResponse,
		url: URL,
		bucketName: string,
	): Promise<void> {
		this.counters.init++;
		if (!bucketName) return this.fail(res, 400, '未传入bucketName');

		const form = await readMultipart(req);
		const pathname = normalize(str(form, 'pathname'));
		const totalMd5 = str(form, 'totalMd5');
		const totalSize = Number(str(form, 'totalSize') || 0);
		const chunks = Number(str(form, 'chunks') || 0);
		// 服务端用 @RequestParam 读取这两个字段
		const cover = url.searchParams.get('cover') === 'true';
		const isPublic = url.searchParams.get('isPublic') === 'true';

		if (!pathname) return this.fail(res, 400, 'pathname不能为空');
		if (!totalMd5) return this.fail(res, 400, 'totalMd5不能为空');
		if (!(chunks > 0)) return this.fail(res, 400, 'chunks必须大于0');

		try {
			this.checkSign(url, pathname);
		} catch (e) {
			return this.fail(res, 403, (e as Error).message);
		}

		// 重名检查（服务端在秒传之前）
		if (!cover && this.resources.has(pathname)) {
			return this.fail(res, 400, '已存在相同的pathname');
		}

		// 秒传：md5 + size 已存在且合并完成
		const existing = [...this.fileHouses.values()].find((f) => f.md5 === totalMd5 && f.size === totalSize && f.merged);
		const fileHouseId = md5Of(`${totalMd5}:${totalSize}`);

		if (existing) {
			this.resources.set(pathname, {
				pathname,
				fileHouseId,
				sizeByte: totalSize,
				lastModified: Number(str(form, 'lastModified') || Date.now()),
				contentType: str(form, 'contentType') || undefined,
				isPublic,
			});
			return this.json(res, 200, {
				status: 'ok',
				result: { fileExists: true, existedChunkIndexs: Array.from({ length: chunks }, (_, i) => i) },
			});
		}

		const fh: FileHouse = this.fileHouses.get(fileHouseId) ?? {
			md5: totalMd5,
			size: totalSize,
			chunks: new Array(chunks),
			merged: false,
		};
		this.fileHouses.set(fileHouseId, fh);
		this.pending.set(pathname, { totalMd5, totalSize, chunks, pathname });

		// 断点续传：返回已上传的分片
		const existed = fh.chunks.map((c, i) => (c ? i : -1)).filter((i) => i >= 0);

		return this.json(res, 200, {
			status: 'ok',
			result: { fileExists: false, existedChunkIndexs: existed },
		});
	}

	private async handleChunk(
		req: http.IncomingMessage,
		res: http.ServerResponse,
		url: URL,
		bucketName: string,
	): Promise<void> {
		this.counters.chunk++;
		const form = await readMultipart(req);
		const pathname = normalize(str(form, 'pathname'));
		const totalMd5 = str(form, 'totalMd5');
		const chunkIndex = Number(str(form, 'chunkIndex'));
		const chunkMd5 = str(form, 'chunkMd5');
		const file = buf(form, 'file');

		if (chunkIndex === undefined || Number.isNaN(chunkIndex)) {
			return this.fail(res, 400, 'chunkIndex不能为空');
		}
		if (!file) return this.fail(res, 400, '缺少文件内容');

		try {
			this.checkSign(url, pathname);
		} catch (e) {
			return this.fail(res, 403, (e as Error).message);
		}

		const fileHouseId = md5Of(`${totalMd5}:${Number(str(form, 'totalSize') || 0)}`);
		const fh = this.fileHouses.get(fileHouseId);
		if (!fh) return this.fail(res, 400, '未初始化，请先调用 /init');

		// 校验分片 md5
		const actualMd5 = crypto.createHash('md5').update(file).digest('hex');
		if (chunkMd5 && actualMd5 !== chunkMd5) {
			return this.fail(res, 400, `分片${chunkIndex} md5不匹配：期望 ${chunkMd5}，实际 ${actualMd5}`);
		}

		fh.chunks[chunkIndex] = { data: file, chunkMd5: actualMd5 };
		return this.json(res, 200, { status: 'ok', result: null });
	}

	private async handleMerge(
		req: http.IncomingMessage,
		res: http.ServerResponse,
		url: URL,
	): Promise<void> {
		this.counters.merge++;
		const form = await readMultipart(req);
		const pathname = normalize(str(form, 'pathname'));
		const totalMd5 = str(form, 'totalMd5');
		const totalSize = Number(str(form, 'totalSize') || 0);
		const chunks = Number(str(form, 'chunks') || 0);
		const isPublic = url.searchParams.get('isPublic') === 'true';

		try {
			this.checkSign(url, pathname);
		} catch (e) {
			return this.fail(res, 403, (e as Error).message);
		}

		const fileHouseId = md5Of(`${totalMd5}:${totalSize}`);
		const fh = this.fileHouses.get(fileHouseId);
		if (!fh) return this.fail(res, 400, '未初始化');

		for (let i = 0; i < chunks; i++) {
			if (!fh.chunks[i]) return this.fail(res, 400, `分片${i}缺失，无法合并`);
		}

		const merged = Buffer.concat(fh.chunks.map((c) => c!.data));
		if (merged.length !== totalSize) {
			return this.fail(res, 400, `合并后大小不匹配：期望 ${totalSize}，实际 ${merged.length}`);
		}

		const actualMd5 = crypto.createHash('md5').update(merged).digest('hex');
		if (totalMd5 && actualMd5 !== totalMd5) {
			return this.fail(res, 400, `合并后 md5 不匹配：期望 ${totalMd5}，实际 ${actualMd5}`);
		}

		fh.merged = true;
		this.resources.set(pathname, {
			pathname,
			fileHouseId,
			sizeByte: totalSize,
			lastModified: Number(str(form, 'lastModified') || Date.now()),
			contentType: str(form, 'contentType') || undefined,
			isPublic,
		});

		return this.json(res, 200, { status: 'ok', result: null });
	}

	// ------------------------------------------------------------ 查询

	private handleIsExists(res: http.ServerResponse, url: URL, bucketName: string): void {
		const pathname = normalize(url.searchParams.get('pathname') ?? '');
		try {
			this.checkSign(url, pathname);
		} catch (e) {
			return this.fail(res, 403, (e as Error).message);
		}
		return this.json(res, 200, { status: 'ok', result: this.resources.has(pathname) });
	}

	private handleInfo(res: http.ServerResponse, url: URL, bucketName: string): void {
		this.counters.info++;
		const pathname = normalize(url.searchParams.get('pathname') ?? '');
		try {
			this.checkSign(url, pathname);
		} catch (e) {
			return this.fail(res, 403, (e as Error).message);
		}
		const resource = this.resources.get(pathname);
		if (!resource) return this.fail(res, 400, `资源${pathname}不存在`);
		const fh = this.fileHouses.get(resource.fileHouseId);
		return this.json(res, 200, {
			status: 'ok',
			result: {
				...resource,
				md5: fh?.md5,
				name: pathname.split('/').pop(),
				isDir: false,
			},
		});
	}

	private handleList(res: http.ServerResponse, url: URL, bucketName: string): void {
		this.counters.list++;
		const path = normalize(url.searchParams.get('path') ?? '/');
		try {
			this.checkSign(url, path);
		} catch (e) {
			return this.fail(res, 403, (e as Error).message);
		}

		const prefix = path === '/' ? '/' : path + '/';
		const list: Array<Record<string, unknown>> = [];
		const seenDirs = new Set<string>();

		// 返回结构对齐服务端 DirAndResource：
		// 使用 isDir 区分目录，名称在 fileName，目录 sizeByte 为 null
		for (const [pathname, resource] of this.resources) {
			if (!pathname.startsWith(prefix)) continue;
			const rel = pathname.slice(prefix.length);
			if (!rel) continue;
			const slash = rel.indexOf('/');
			if (slash >= 0) {
				const dirName = rel.slice(0, slash);
				if (!seenDirs.has(dirName)) {
					seenDirs.add(dirName);
					list.push({
						isDir: true,
						path: `${prefix}${dirName}`,
						fileName: dirName,
						sizeByte: null,
						lastModified: 0,
					});
				}
			} else {
				list.push({
					isDir: false,
					path: pathname,
					fileName: rel,
					sizeByte: resource.sizeByte,
					lastModified: resource.lastModified,
					contentType: resource.contentType ?? null,
				});
			}
		}

		// 支持服务端 keyWord 语法：f:文件名 p:路径 e:排除 ef:路径+文件名排除
		const keyWord = url.searchParams.get('keyWord');
		let filtered = list;
		if (keyWord) {
			filtered = list.filter((i) => matchesKeyWord(i, keyWord, path));
		}

		return this.json(res, 200, {
			status: 'ok',
			result: { list: filtered, total: filtered.length, pageNum: 1, pageSize: filtered.length },
		});
	}

	private handleDeleteFile(
		req: http.IncomingMessage,
		res: http.ServerResponse,
		url: URL,
		bucketName: string,
	): void {
		this.counters.delete++;
		const pathname = normalize(url.searchParams.get('pathname') ?? '');
		try {
			this.checkSign(url, pathname);
		} catch (e) {
			return this.fail(res, 403, (e as Error).message);
		}
		const existed = this.resources.delete(pathname);
		return this.json(res, 200, { status: 'ok', result: existed });
	}

	private handleRename(req: http.IncomingMessage, res: http.ServerResponse, url: URL, bucketName: string): void {
		const pathname = normalize(url.searchParams.get('pathname') ?? '');
		const desPathname = normalize(url.searchParams.get('desPathname') ?? '');
		if (!pathname || !desPathname) return this.fail(res, 400, 'pathname不能为空');

		// 服务端对两个路径都做签名校验，这里只校验原路径
		try {
			this.checkSign(url, pathname);
		} catch (e) {
			return this.fail(res, 403, (e as Error).message);
		}

		const resource = this.resources.get(pathname);
		if (!resource) return this.fail(res, 400, `资源${pathname}不存在`);
		this.resources.delete(pathname);
		this.resources.set(desPathname, { ...resource, pathname: desPathname });
		return this.json(res, 200, { status: 'ok', result: null });
	}

	private handleDeleteDir(req: http.IncomingMessage, res: http.ServerResponse, url: URL, bucketName: string): void {
		const pathname = normalize(url.searchParams.get('pathname') ?? '');
		try {
			this.checkSign(url, pathname);
		} catch (e) {
			return this.fail(res, 403, (e as Error).message);
		}
		const prefix = pathname === '/' ? '/' : pathname + '/';
		let count = 0;
		for (const key of [...this.resources.keys()]) {
			if (key.startsWith(prefix)) {
				this.resources.delete(key);
				count++;
			}
		}
		return this.json(res, 200, { status: 'ok', result: count > 0 });
	}

	// ------------------------------------------------------------ 下载

	private handleDownload(
		req: http.IncomingMessage,
		res: http.ServerResponse,
		url: URL,
		decodedPath: string,
	): void {
		this.counters.download++;
		const rest = decodedPath.slice('/mos/'.length);
		const slash = rest.indexOf('/');
		const bucketName = slash >= 0 ? rest.slice(0, slash) : rest;
		const pathname = slash >= 0 ? rest.slice(slash) : '/';

		// 若资源是公开的，无需签名；否则校验
		const resource = this.resources.get(pathname);
		if (!resource?.isPublic) {
			try {
				this.checkSign(url, pathname);
			} catch (e) {
				return this.fail(res, 403, (e as Error).message);
			}
		}

		if (!resource) return this.fail(res, 404, `资源${pathname}不存在`);

		const fh = this.fileHouses.get(resource.fileHouseId);
		const content = fh ? Buffer.concat(fh.chunks.map((c) => c.data)) : Buffer.alloc(0);

		res.writeHead(200, {
			'Content-Type': resource.contentType ?? 'application/octet-stream',
			'Content-Length': String(content.length),
			'Access-Control-Allow-Origin': '*',
			'Last-Modified': new Date(resource.lastModified).toUTCString(),
		});
		res.end(content);
	}

	// ------------------------------------------------------------ 工具

	private json(res: http.ServerResponse, code: number, body: unknown): void {
		const payload = Buffer.from(JSON.stringify(body), 'utf8');
		res.writeHead(code, { 'Content-Type': 'application/json;charset=utf-8', 'Content-Length': String(payload.length) });
		res.end(payload);
	}

	private fail(res: http.ServerResponse, code: number, message: string): void {
		this.json(res, code, { status: 'error', message, result: null });
	}
}

class HttpError extends Error {
	constructor(
		readonly code: number,
		message: string,
	) {
		super(message);
	}
}

// ------------------------------------------------------------ 辅助函数

function normalize(p: string): string {
	let s = (p ?? '').trim().replace(/\\/g, '/');
	if (!s.startsWith('/')) s = '/' + s;
	return s.replace(/\/{2,}/g, '/');
}

/**
 * 按服务端语法过滤列表项。
 * f:文件名包含 / p:路径包含 / e:排除 / ef:路径包含且文件名排除
 */
function matchesKeyWord(item: Record<string, unknown>, keyWord: string, parentPath: string): boolean {
	const name = String(item.fileName ?? '');
	const fullPath = String(item.path ?? '');

	for (const word of keyWord.split(/\s+/).filter(Boolean)) {
		let prefix = '';
		let kw = word;
		if (/^(ef|ep|f|p|e):/.test(word)) {
			prefix = word.slice(0, word.indexOf(':') + 1);
			kw = word.slice(word.indexOf(':') + 1);
		}
		if (!kw) continue;
		switch (prefix) {
			case 'p:':
				if (!fullPath.includes(kw)) return false;
				break;
			case 'e:':
				if (fullPath.includes(kw) || name.includes(kw)) return false;
				break;
			case 'ef:':
				if (!fullPath.includes(kw) || name.includes(kw)) return false;
				break;
			default:
				// f: 与裸关键字都按文件名匹配
				if (!name.includes(kw)) return false;
		}
	}
	return true;
}

function md5Of(s: string): string {
	return crypto.createHash('md5').update(s).digest('hex');
}

/**
 * 等价于 Java 的 URLDecoder.decode(s, "UTF-8")：
 * 解 %XX，并把 + 还原成空格。
 */
function javaUrlDecode(input: string): string {
	try {
		return decodeURIComponent(input.replace(/\+/g, ' '));
	} catch {
		return input;
	}
}

interface MultipartFields {
	[name: string]: string | Buffer | undefined;
}

/** 从 multipart 字段中取字符串值 */
function str(form: MultipartFields, key: string): string {
	const v = form[key];
	return typeof v === 'string' ? v : '';
}

/** 从 multipart 字段中取 Buffer 值 */
function buf(form: MultipartFields, key: string): Buffer | undefined {
	const v = form[key];
	return Buffer.isBuffer(v) ? v : undefined;
}

/** 解析 multipart/form-data（服务端会把所有参数当作表单字段） */
async function readMultipart(req: http.IncomingMessage): Promise<MultipartFields> {
	const contentType = req.headers['content-type'] ?? '';
	const m = /boundary=(?:"([^"]+)"|([^;]+))/i.exec(contentType);
	if (!m) return {};
	const boundary = `--${m[1] ?? m[2]}`;

	const chunks: Buffer[] = [];
	for await (const c of req) chunks.push(c as Buffer);
	const body = Buffer.concat(chunks);

	const fields: MultipartFields = {};
	let pos = body.indexOf(boundary);
	while (pos >= 0) {
		const start = pos + boundary.length;
		// 结束标记
		if (body.slice(start, start + 2).toString() === '--') break;

		const headerEnd = body.indexOf('\r\n\r\n', start);
		if (headerEnd < 0) break;
		const headers = body.slice(start, headerEnd).toString('utf8');

		let next = body.indexOf(boundary, headerEnd);
		if (next < 0) next = body.length;
		// 去掉分隔前的 \r\n
		const contentEnd = next - 2;
		const content = body.slice(headerEnd + 4, contentEnd);

		const nameMatch = /name="([^"]*)"/i.exec(headers);
		if (nameMatch) {
			const filenameMatch = /filename="([^"]*)"/i.exec(headers);
			if (filenameMatch) {
				fields[nameMatch[1]] = content;
			} else {
				fields[nameMatch[1]] = content.toString('utf8');
			}
		}

		pos = next;
	}
	return fields;
}
