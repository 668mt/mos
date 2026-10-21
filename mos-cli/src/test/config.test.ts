/**
 * 配置相关测试：逐项合并、profile 隔离、默认值、参数优先级。
 */

import test from 'node:test';
import assert from 'node:assert/strict';
import * as fs from 'node:fs';
import * as os from 'node:os';
import * as path from 'node:path';
import { loadConfigFile, resolveConfig, saveConfigFile, emptyConfig } from '../core/config.js';

/** 在独立配置文件上跑测试，避免污染真实配置 */
function withConfigFile<T>(fn: (file: string) => T): T {
	const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'mos-cfg-'));
	const file = path.join(dir, 'config.json');
	const prev = process.env.MOS_CONFIG;
	process.env.MOS_CONFIG = file;
	try {
		return fn(file);
	} finally {
		if (prev === undefined) delete process.env.MOS_CONFIG;
		else process.env.MOS_CONFIG = prev;
		fs.rmSync(dir, { recursive: true, force: true });
	}
}

/** 清掉可能干扰的环境变量 */
function clearEnv(): void {
	for (const k of ['MOS_HOST', 'MOS_BUCKET', 'MOS_SECRET_KEY', 'MOS_OPEN_ID', 'MOS_PROFILE']) {
		delete process.env[k];
	}
}

test('resolveConfig 默认使用 default 档案', () => {
	withConfigFile(() => {
		clearEnv();
		const cfg = emptyConfig();
		cfg.defaultProfile = 'default';
		cfg.profiles = {
			default: { host: 'http://d:1', bucket: 'bd', secretKey: 'sd', openId: 1 },
			dev: { host: 'http://dev:2', bucket: 'bv', secretKey: 'sv', openId: 2 },
		};
		saveConfigFile(cfg);

		const r = resolveConfig();
		assert.equal(r.profile, 'default');
		assert.equal(r.host, 'http://d:1');
		assert.equal(r.bucket, 'bd');
	});
});

test('resolveConfig 按 profile 切换档案', () => {
	withConfigFile(() => {
		clearEnv();
		const cfg = emptyConfig();
		cfg.defaultProfile = 'default';
		cfg.profiles = {
			default: { host: 'http://d:1', bucket: 'bd', secretKey: 'sd', openId: 1 },
			dev: { host: 'http://dev:2', bucket: 'bv', secretKey: 'sv', openId: 2 },
		};
		saveConfigFile(cfg);

		const r = resolveConfig({ profile: 'dev' });
		assert.equal(r.profile, 'dev');
		assert.equal(r.host, 'http://dev:2');
		assert.equal(r.bucket, 'bv');
		assert.equal(r.openId, 2);
	});
});

test('不存在的 profile 退化为空配置而不是报错', () => {
	withConfigFile(() => {
		clearEnv();
		const cfg = emptyConfig();
		cfg.defaultProfile = 'default';
		cfg.profiles = { default: { host: 'http://d:1', bucket: 'b', secretKey: 's', openId: 0 } };
		saveConfigFile(cfg);

		const r = resolveConfig({ profile: 'ghost' });
		assert.equal(r.profile, 'ghost');
		assert.equal(r.host, '');
	});
});

test('命令行参数优先于环境变量与配置文件', () => {
	withConfigFile(() => {
		clearEnv();
		process.env.MOS_HOST = 'http://env:9';
		const cfg = emptyConfig();
		cfg.profiles = { default: { host: 'http://file:8', bucket: 'fb', secretKey: 'fs', openId: 0 } };
		saveConfigFile(cfg);

		// 命令行覆盖环境变量
		assert.equal(resolveConfig({ host: 'http://arg:7' }).host, 'http://arg:7');
		// 环境变量覆盖配置文件
		assert.equal(resolveConfig().host, 'http://env:9');
	});
});

test('MOS_PROFILE 环境变量可切换档案', () => {
	withConfigFile(() => {
		clearEnv();
		const cfg = emptyConfig();
		cfg.profiles = {
			default: { host: 'http://d:1', bucket: 'bd', secretKey: 'sd', openId: 0 },
			prod: { host: 'http://p:3', bucket: 'bp', secretKey: 'sp', openId: 0 },
		};
		saveConfigFile(cfg);
		process.env.MOS_PROFILE = 'prod';
		try {
			const r = resolveConfig();
			assert.equal(r.profile, 'prod');
			assert.equal(r.host, 'http://p:3');
		} finally {
			delete process.env.MOS_PROFILE;
		}
	});
});

test('openId 0 是有效值，不会被当成未设置', () => {
	withConfigFile(() => {
		clearEnv();
		const cfg = emptyConfig();
		cfg.profiles = { default: { host: 'h', bucket: 'b', secretKey: 's', openId: 0 } };
		saveConfigFile(cfg);
		assert.equal(resolveConfig().openId, 0);
		// 显式传 0 也应保留
		assert.equal(resolveConfig({ openId: 0 }).openId, 0);
	});
});

test('配置文件不存在时返回空配置', () => {
	const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'mos-cfg-'));
	const prev = process.env.MOS_CONFIG;
	process.env.MOS_CONFIG = path.join(dir, 'nope.json');
	try {
		const cfg = loadConfigFile();
		assert.deepEqual(cfg.profiles, {});
	} finally {
		if (prev === undefined) delete process.env.MOS_CONFIG;
		else process.env.MOS_CONFIG = prev;
		fs.rmSync(dir, { recursive: true, force: true });
	}
});
