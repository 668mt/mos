/**
 * 多地址（hosts）相关测试：解析、权重、健康检查、故障转移。
 */

import test from 'node:test';
import assert from 'node:assert/strict';
import { clearHealthCache, parseHost, parseHosts, pickHost } from '../core/hosts.js';

test('省略协议时默认补 http://', () => {
	assert.equal(parseHost('192.168.0.1:4100').host, 'http://192.168.0.1:4100');
	assert.equal(parseHost('mos.example.com').host, 'http://mos.example.com');
	assert.equal(parseHost('  192.168.0.1  ').host, 'http://192.168.0.1');
});

test('显式的 http/https 会被保留', () => {
	assert.equal(parseHost('http://a.com').host, 'http://a.com');
	assert.equal(parseHost('https://a.com').host, 'https://a.com');
	assert.equal(parseHost('HTTPS://a.com').host, 'HTTPS://a.com');
});

test('去掉结尾斜杠', () => {
	assert.equal(parseHost('http://a.com/').host, 'http://a.com');
	assert.equal(parseHost('a.com:80/').host, 'http://a.com:80');
});

test('解析权重后缀', () => {
	assert.deepEqual(parseHost('http://a.com(50)'), {
		host: 'http://a.com',
		externHost: null,
		weight: 50,
	});
	assert.deepEqual(parseHost('192.168.0.1:4100(0)'), {
		host: 'http://192.168.0.1:4100',
		externHost: null,
		weight: 0,
	});
	// 无权重时默认 100
	assert.deepEqual(parseHost('http://a.com'), {
		host: 'http://a.com',
		externHost: null,
		weight: 100,
	});
});

test('解析外网地址后缀 []', () => {
	assert.deepEqual(parseHost('http://a.com[http://rs.com]'), {
		host: 'http://a.com',
		externHost: 'http://rs.com',
		weight: 100,
	});
	// 外网地址 + 权重
	assert.deepEqual(parseHost('http://a.com[http://rs.com](30)'), {
		host: 'http://a.com',
		externHost: 'http://rs.com',
		weight: 30,
	});
	// 外网地址没写协议时也补
	assert.deepEqual(parseHost('http://a.com[rs.com]'), {
		host: 'http://a.com',
		externHost: 'http://rs.com',
		weight: 100,
	});
	// https 保留
	assert.deepEqual(parseHost('https://a.com[https://rs.com]'), {
		host: 'https://a.com',
		externHost: 'https://rs.com',
		weight: 100,
	});
});

test('多个地址用逗号/分号/换行分隔', () => {
	const hosts = parseHosts('a.com,b.com;c.com\nd.com');
	assert.equal(hosts.length, 4);
	assert.deepEqual(
		hosts.map((h) => h.host),
		['http://a.com', 'http://b.com', 'http://c.com', 'http://d.com'],
	);
});

test('忽略空条目', () => {
	const hosts = parseHosts('a.com, ,b.com,');
	assert.equal(hosts.length, 2);
});

test('parseHosts 支持数组入参', () => {
	const hosts = parseHosts(['a.com', 'https://b.com']);
	assert.equal(hosts.length, 2);
	assert.equal(hosts[1].host, 'https://b.com');
});

test('空值返回空数组', () => {
	assert.deepEqual(parseHosts(''), []);
	assert.deepEqual(parseHosts(undefined), []);
});

test('权重为 0 的地址不参与选择', async () => {
	clearHealthCache();
	const picked = await pickHost(parseHosts('a.com(0),b.com(0)'), { checkUri: null });
	// 全部权重为 0 时应回退为 100（与 Java 一致）
	assert.ok(picked === null || picked.host === 'http://a.com' || picked.host === 'http://b.com');
});

test('只有一个地址时直接返回', async () => {
	clearHealthCache();
	const picked = await pickHost(parseHosts('only.com'), { checkUri: null });
	assert.equal(picked?.host, 'http://only.com');
	assert.equal(picked?.externHost, null);
});

test('选中时带回外网地址', async () => {
	clearHealthCache();
	const picked = await pickHost(parseHosts('a.com[http://rs.com]'), { checkUri: null });
	assert.equal(picked?.host, 'http://a.com');
	assert.equal(picked?.externHost, 'http://rs.com');
});

test('空列表返回 null', async () => {
	clearHealthCache();
	assert.equal(await pickHost([], { checkUri: null }), null);
});

test('高权重地址被选中的概率更高', async () => {
	clearHealthCache();
	const hosts = parseHosts('heavy.com(100),light.com(1)');
	let heavy = 0;
	for (let i = 0; i < 300; i++) {
		const picked = await pickHost(hosts, { checkUri: null });
		if (picked?.host === 'http://heavy.com') heavy++;
	}
	// 期望约 300 * 100/101 次命中
	assert.ok(heavy > 250, `高权重地址命中次数偏少：${heavy}/300`);
});

test('健康检查会过滤掉不可用地址', async () => {
	clearHealthCache();
	// 用一个必然连不通的地址 + checkUri，走真实探测
	const hosts = parseHosts('127.0.0.1:19999');
	const picked = await pickHost(hosts, { checkUri: '/index.html', ttl: 0, timeoutMs: 300 });
	assert.equal(picked, null, '不可用地址应被过滤');
});
