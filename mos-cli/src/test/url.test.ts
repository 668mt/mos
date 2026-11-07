/**
 * 链接地址选择相关测试，与 Java SDK 的 UrlBuildParams 规则保持一致：
 *   1. 显式指定地址（domain）→ 优先使用
 *   2. inner=true → 内网地址
 *   3. 否则 → 外网地址（没配 [] 时回退到内网地址）
 */

import test from 'node:test';
import assert from 'node:assert/strict';
import { MosClient } from '../core/client.js';
import { ResolvedConfig } from '../core/config.js';
import { parseHosts } from '../core/hosts.js';

function makeClient(host: string): MosClient {
	const config: ResolvedConfig = {
		profile: 'default',
		host,
		hosts: parseHosts(host),
		bucket: 'test-bucket',
		secretKey: 'test-secret',
		openId: 1,
		sources: {},
	};
	return new MosClient(config);
}

test('默认用外网地址生成链接', () => {
	const client = makeClient('http://10.0.0.1:4100[http://mos.example.com]');
	const url = client.getResourceUrl('/a/b.txt');
	assert.ok(url.startsWith('http://mos.example.com/mos/test-bucket'), url);
});

test('没配外网地址时回退到内网地址', () => {
	const client = makeClient('http://10.0.0.1:4100');
	const url = client.getResourceUrl('/a/b.txt');
	assert.ok(url.startsWith('http://10.0.0.1:4100/mos/test-bucket'), url);
});

test('--inner 使用内网地址生成链接', () => {
	const client = makeClient('http://10.0.0.1:4100[http://mos.example.com]');
	const url = client.getResourceUrl('/a/b.txt', { inner: true });
	assert.ok(url.startsWith('http://10.0.0.1:4100/mos/test-bucket'), url);
});

test('--domain 显式指定的地址优先于内外网', () => {
	const client = makeClient('http://10.0.0.1:4100[http://mos.example.com]');
	const url = client.getResourceUrl('/a/b.txt', { domain: 'http://custom.example.com/' });
	assert.ok(url.startsWith('http://custom.example.com/mos/test-bucket'), url);
});

test('resolveLinkHost 的选择规则', () => {
	const client = makeClient('http://10.0.0.1:4100[http://mos.example.com]');
	assert.equal(client.resolveLinkHost(), 'http://mos.example.com');
	assert.equal(client.resolveLinkHost({ inner: true }), 'http://10.0.0.1:4100');
	assert.equal(client.resolveLinkHost({ domain: 'http://custom.example.com/' }), 'http://custom.example.com');
	// 显式地址优先于 inner
	assert.equal(
		client.resolveLinkHost({ inner: true, domain: 'http://custom.example.com' }),
		'http://custom.example.com',
	);
});
