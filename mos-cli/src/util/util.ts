/** 通用小工具 */

import * as path from 'node:path';
import * as os from 'node:os';
import { createHash } from 'node:crypto';
import { createReadStream } from 'node:fs';

/**
 * 统一 pathname：
 *   - 反斜杠转正斜杠
 *   - 补全开头的 /
 *   - 折叠重复斜杠
 *   - 解析 . 与 .. 片段（mos ls . 会被规范化成 /）
 *   - 去掉结尾的 /（根目录除外）
 */
export function normalizePathname(input: string): string {
	let p = (input ?? '').trim().replace(/\\/g, '/');
	if (!p.startsWith('/')) p = '/' + p;

	// 折叠重复斜杠并解析 . / ..
	const resolved: string[] = [];
	for (const seg of p.split('/')) {
		if (seg === '' || seg === '.') continue;
		if (seg === '..') {
			resolved.pop();
			continue;
		}
		resolved.push(seg);
	}

	const joined = '/' + resolved.join('/');
	// 根目录或以 / 结尾时保留结尾斜杠由调用方决定，这里统一去掉
	return joined === '/' ? '/' : joined;
}

/** 拼接 pathname */
export function joinPathname(...parts: string[]): string {
	return normalizePathname(parts.filter(Boolean).join('/'));
}

/** 取文件名 */
export function basename(p: string): string {
	const parts = normalizePathname(p).split('/');
	return parts[parts.length - 1] || '/';
}

/** 取父目录 */
export function dirname(p: string): string {
	const parts = normalizePathname(p).split('/');
	parts.pop();
	const joined = parts.join('/');
	return joined === '' ? '/' : joined;
}

/**
 * 逐段 URL 编码，与 Java `PathnameDefine.urlEncodedPathname` 保持一致。
 *
 * Java 用的是 URLEncoder.encode（application/x-www-form-urlencoded），
 * 与 encodeURIComponent 有三处关键差异：
 *   1. 空格编码成 `+`，而不是 `%20`
 *   2. `*` `-` `_` `.` 不编码
 *   3. `!` `'` `(` `)` `~` 会被编码
 * 必须严格对齐，否则服务端反解出来的 pathname 与签名内容不一致，
 * 会导致签名校验失败或 404。
 */
export function encodePathname(pathname: string): string {
	return normalizePathname(pathname)
		.split('/')
		.map((seg) => javaUrlEncode(seg))
		.join('/');
}

/**
 * 等价于 Java 的 URLEncoder.encode(s, "UTF-8")。
 */
export function javaUrlEncode(input: string): string {
	let out = '';
	const bytes = Buffer.from(input, 'utf8');
	for (const b of bytes) {
		const c = String.fromCharCode(b);
		if ((b >= 0x41 && b <= 0x5a) || (b >= 0x61 && b <= 0x7a) || (b >= 0x30 && b <= 0x39)) {
			// A-Z a-z 0-9
			out += c;
		} else if (c === ' ') {
			out += '+';
		} else if (c === '-' || c === '_' || c === '.' || c === '*') {
			out += c;
		} else {
			out += '%' + b.toString(16).toUpperCase().padStart(2, '0');
		}
	}
	return out;
}

/** 计算文件 MD5（十六进制小写），对应 Java 的 DigestUtils.md5Hex */
export async function md5File(filePath: string): Promise<string> {
	return new Promise((resolve, reject) => {
		const hash = createHash('md5');
		const stream = createReadStream(filePath);
		stream.on('error', reject);
		stream.on('data', (chunk) => hash.update(chunk));
		stream.on('end', () => resolve(hash.digest('hex')));
	});
}

/**
 * 计算字节缓冲的 MD5，用于分片校验。
 */
export function md5Buffer(buf: Buffer): string {
	return createHash('md5').update(buf).digest('hex');
}

/**
 * 简单的并发控制（用于分片并行上传）
 */
export async function concurrentMap<T, R>(
	items: T[],
	limit: number,
	worker: (item: T, index: number) => Promise<R>,
): Promise<R[]> {
	const results: R[] = new Array(items.length);
	let cursor = 0;

	const runners = Array.from({ length: Math.max(1, Math.min(limit, items.length)) }, async () => {
		for (;;) {
			const index = cursor++;
			if (index >= items.length) return;
			results[index] = await worker(items[index], index);
		}
	});

	await Promise.all(runners);
	return results;
}

/** 展开 ~ 开头的主目录路径 */
export function expandHome(p: string): string {
	if (p === '~') return os.homedir();
	if (p.startsWith('~/') || p.startsWith('~\\')) {
		return path.join(os.homedir(), p.slice(2));
	}
	return p;
}

/** 取绝对路径 */
export function resolvePath(p: string): string {
	return path.resolve(expandHome(p));
}
