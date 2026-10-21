/**
 * URL 编码与 Java URLEncoder 的字节级一致性测试。
 *
 * 基准数据由 Java 生成：
 *   URLEncoder.encode(s, "UTF-8")
 *
 * 这点很关键：服务端用 Java 的 URLEncoder 反解 pathname，
 * 如果客户端编码规则不一致，签名内容与实际路径就对不上，
 * 会导致签名校验失败或 404。
 */

import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import * as path from 'node:path';
import { encodePathname, javaUrlEncode, normalizePathname } from '../util/util.js';

const refFile = path.join(path.dirname(fileURLToPath(import.meta.url)), 'enc-ref.txt');

interface EncCase {
	input: string;
	expected: string;
}

const cases: EncCase[] = readFileSync(refFile, 'utf8')
	.split(/\r?\n/)
	.map((l) => l.trim())
	.filter(Boolean)
	.map((line) => {
		// 输入本身可能含 |（如 a|b），所以要从最后一个 | 分割
		const idx = line.lastIndexOf('|');
		const raw = line.slice(0, idx);
		const expected = line.slice(idx + 1);
		// 文件里把制表符写成了字面量 \t
		const input = raw.replace(/\\t/g, '\t');
		return { input, expected };
	});

test('基准数据非空', () => {
	assert.ok(cases.length >= 30, `基准数据只有 ${cases.length} 条`);
});

test('javaUrlEncode 与 Java URLEncoder 完全一致', () => {
	for (const { input, expected } of cases) {
		assert.equal(
			javaUrlEncode(input),
			expected,
			`输入 ${JSON.stringify(input)}：期望 ${expected}，实际 ${javaUrlEncode(input)}`,
		);
	}
});

test('空格编码为 + 而不是 %20', () => {
	assert.equal(javaUrlEncode('a b'), 'a+b');
	// 这点与 encodeURIComponent 不同
	assert.notEqual(javaUrlEncode('a b'), encodeURIComponent('a b'));
});

test('Java 不编码的字符', () => {
	assert.equal(javaUrlEncode('a*b'), 'a*b');
	assert.equal(javaUrlEncode('a-b_c.d'), 'a-b_c.d');
});

test('Java 会编码的字符（与 encodeURIComponent 相反）', () => {
	// encodeURIComponent 不编码这些，Java 会编码
	assert.equal(javaUrlEncode('a!'), 'a%21');
	assert.equal(javaUrlEncode("a'"), 'a%27');
	assert.equal(javaUrlEncode('a('), 'a%28');
	assert.equal(javaUrlEncode('a)'), 'a%29');
	assert.equal(javaUrlEncode('a~'), 'a%7E');
});

test('加号本身被编码', () => {
	assert.equal(javaUrlEncode('a+b'), 'a%2Bb');
});

test('非 ASCII 按 UTF-8 逐字节编码', () => {
	assert.equal(javaUrlEncode('中文'), '%E4%B8%AD%E6%96%87');
	assert.equal(javaUrlEncode('🎉'), '%F0%9F%8E%89');
});

test('encodePathname 逐段编码并保留斜杠', () => {
	assert.equal(encodePathname('/a/b.txt'), '/a/b.txt');
	assert.equal(encodePathname('a/b'), '/a/b');
	// 空格在路径里是 +
	assert.equal(encodePathname('/Break Time(111)'), '/Break+Time%28111%29');
	assert.equal(encodePathname('/用户作品/子目录'), '/%E7%94%A8%E6%88%B7%E4%BD%9C%E5%93%81/%E5%AD%90%E7%9B%AE%E5%BD%95');
});

test('encodePathname 与逐段拼接结果一致', () => {
	const p = '/resources/pixiv/用户作品/79293679/Break Time(111564611)/001.jpg';
	const manual = '/' + p.split('/').filter(Boolean).map(javaUrlEncode).join('/');
	assert.equal(encodePathname(p), manual);
	assert.equal(
		encodePathname(p),
		'/resources/pixiv/%E7%94%A8%E6%88%B7%E4%BD%9C%E5%93%81/79293679/Break+Time%28111564611%29/001.jpg',
	);
});

test('编码后再解码能还原原路径', () => {
	const p = '/resources/pixiv/用户作品/Break Time(111564611)/001.jpg';
	const encoded = encodePathname(p);
	// 服务端用 URLDecoder.decode 还原（+ -> 空格）
	const decoded = encoded
		.split('/')
		.map((seg) => decodeURIComponent(seg.replace(/\+/g, ' ')))
		.join('/');
	assert.equal(decoded, p);
});

test('normalizePathname 与编码配合', () => {
	assert.equal(encodePathname(normalizePathname('.')), '/');
	assert.equal(encodePathname(normalizePathname('/a/../b')), '/b');
});
