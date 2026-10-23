/**
 * 命令行参数到核心函数的传参回归测试。
 *
 * 这些问题都曾真实发生过：
 *   - --limit-speed 被 commander 转成 limitSpeed，误读为 flags.limit 导致限速失效
 *   - --profile 在 config show 上不生效
 *   - createClient 传入 program 而非子命令，导致全局选项读不到
 */

import test from 'node:test';
import assert from 'node:assert/strict';
import { Command } from 'commander';
import { collectOverrides } from '../commands/context.js';

/** 构造一个带全局选项的子命令，模拟真实解析 */
function parse(argv: string[]): { opts: Record<string, unknown>; overrides: ReturnType<typeof collectOverrides> } {
	const program = new Command();
	program.exitOverride();

	const cmd = program
		.command('download')
		.argument('<pathname>')
		.option('-o, --out <path>')
		.option('-c, --cover', '', false)
		.option('--limit-speed <kb>', '', (v) => parseInt(v, 10))
		.option('--stdout', '', false)
		.option('--thumb', '', false)
		.option('-r, --recursive', '', false)
		.action(() => {});

	// 与 addGlobalOptions 保持一致
	cmd.option('--host <url>').option('--bucket <name>').option('--secret-key <key>').option('--profile <name>');

	program.parse(argv, { from: 'user' });
	return { opts: cmd.opts(), overrides: collectOverrides(cmd) };
}

test('--limit-speed 解析为 limitSpeed（不是 limit）', () => {
	const { opts } = parse(['download', '/a.txt', '--limit-speed', '512']);
	assert.equal(opts.limitSpeed, 512, '应解析到 limitSpeed');
	assert.equal(opts.limit, undefined, '不应存在 limit 字段');
});

test('--limit-speed 缺省时为 undefined', () => {
	const { opts } = parse(['download', '/a.txt']);
	assert.equal(opts.limitSpeed, undefined);
});

test('-o/--out 正常解析', () => {
	const { opts } = parse(['download', '/a.txt', '-o', './x']);
	assert.equal(opts.out, './x');
});

test('子命令可接收全局选项 --profile', () => {
	const { overrides } = parse(['download', '/a.txt', '--profile', 'dev']);
	assert.equal(overrides.profile, 'dev');
});

test('collectOverrides 能取到子命令上的全局选项', () => {
	const { overrides } = parse(['download', '/a.txt', '--host', 'http://a:1', '--bucket', 'b', '--secret-key', 'k']);
	assert.equal(overrides.host, 'http://a:1');
	assert.equal(overrides.bucket, 'b');
	assert.equal(overrides.secretKey, 'k');
});

test('未传 --profile 时不产生覆盖（由配置文件决定）', () => {
	const { overrides } = parse(['download', '/a.txt']);
	assert.equal(overrides.profile, undefined);
});

test('其余下载选项解析正常', () => {
	const { opts } = parse(['download', '/a.txt', '--cover', '--stdout', '--thumb', '-r']);
	assert.equal(opts.cover, true);
	assert.equal(opts.stdout, true);
	assert.equal(opts.thumb, true);
	assert.equal(opts.recursive, true);
});