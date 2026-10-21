import test from 'node:test';
import assert from 'node:assert/strict';
import {
	basename,
	concurrentMap,
	dirname,
	encodePathname,
	joinPathname,
	md5Buffer,
	normalizePathname,
	resolvePath,
	expandHome,
} from '../util/util.js';

test('normalizePathname 统一路径格式', () => {
	assert.equal(normalizePathname('.'), '/');
	assert.equal(normalizePathname('./'), '/');
	assert.equal(normalizePathname('/'), '/');
	assert.equal(normalizePathname(''), '/');
	assert.equal(normalizePathname('..'), '/');
	assert.equal(normalizePathname('a/b'), '/a/b');
	assert.equal(normalizePathname('/a/b/'), '/a/b');
	assert.equal(normalizePathname('/a//b'), '/a/b');
	assert.equal(normalizePathname('/a/./b'), '/a/b');
	assert.equal(normalizePathname('/a/../b'), '/b');
	assert.equal(normalizePathname('a\\b'), '/a/b');
	assert.equal(normalizePathname('/中文/文件.png'), '/中文/文件.png');
});

test('normalizePathname 不会把绝对路径吃掉', () => {
	assert.equal(normalizePathname('/a/b/c.txt'), '/a/b/c.txt');
	// 去掉结尾斜杠
	assert.equal(normalizePathname('/dir/'), '/dir');
});

test('basename / dirname', () => {
	assert.equal(basename('/a/b/c.txt'), 'c.txt');
	assert.equal(basename('c.txt'), 'c.txt');
	assert.equal(dirname('/a/b/c.txt'), '/a/b');
	assert.equal(dirname('/c.txt'), '/');
});

test('joinPathname', () => {
	assert.equal(joinPathname('/a', 'b', 'c.txt'), '/a/b/c.txt');
	assert.equal(joinPathname('/a/', '/b/'), '/a/b');
});

test('encodePathname 逐段编码且保留斜杠', () => {
	// 与 Java PathnameDefine.urlEncodedPathname 行为一致：按段用 URLEncoder 编码
	assert.equal(encodePathname('/中文/文件.png'), '/%E4%B8%AD%E6%96%87/%E6%96%87%E4%BB%B6.png');
	// 空格按 Java 规则编码成 +（不是 %20）
	assert.equal(encodePathname('/a b/c.txt'), '/a+b/c.txt');
	assert.equal(encodePathname('/a/b.txt'), '/a/b.txt');
});

test('md5Buffer 与标准 md5 一致', () => {
	assert.equal(md5Buffer(Buffer.from('')), 'd41d8cd98f00b204e9800998ecf8427e');
	assert.equal(md5Buffer(Buffer.from('hello')), '5d41402abc4b2a76b9719d911017c592');
});

test('concurrentMap 保持顺序并全部执行', async () => {
	const items = [1, 2, 3, 4, 5, 6, 7, 8];
	const results = await concurrentMap(items, 3, async (n) => {
		await new Promise((r) => setTimeout(r, 1));
		return n * 2;
	});
	assert.deepEqual(results, [2, 4, 6, 8, 10, 12, 14, 16]);
});

test('concurrentMap 并发数超过元素数也正常', async () => {
	const results = await concurrentMap([1, 2], 10, async (n) => n + 1);
	assert.deepEqual(results, [2, 3]);
});

test('expandHome / resolvePath', () => {
	assert.ok(expandHome('~').length > 0);
	assert.ok(expandHome('~/x').includes('x'));
	assert.ok(resolvePath('.').length > 0);
});
