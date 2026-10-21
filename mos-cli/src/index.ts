#!/usr/bin/env node
/**
 * mos —— MOS 对象存储命令行工具
 */

import { Command } from 'commander';
import { registerConfigCommand } from './commands/config.js';
import { registerInstallCommand } from './commands/install.js';
import { registerUploadCommand } from './commands/upload.js';
import { registerDownloadCommand } from './commands/download.js';
import {
	registerExistsCommand,
	registerInfoCommand,
	registerLsCommand,
	registerMvCommand,
	registerRmCommand,
	registerUrlCommand,
} from './commands/misc.js';
import { error } from './util/ui.js';
import { MosError } from './core/client.js';

const VERSION = '1.0.0';

const program = new Command();

program
	.name('mos')
	.description('MOS 对象存储命令行工具（上传、下载、管理文件）')
	.version(VERSION, '-v, --version', '显示版本号')
	.option('--debug', '输出调试信息', false)
	.showHelpAfterError('（使用 mos --help 查看可用命令）');
// 全局选项（--host/--bucket/--secret-key/--open-id/--profile）不在根命令上声明，
// 而是由 addGlobalOptions 挂到每个数据子命令上。
// 这样既能写 `mos ls --profile dev`，也不会让根命令的 --host
// 抢走 `mos config set --host ...` 的参数。
// 同时支持前置写法：mos --host X ls  由预解析（见 preParse）完成。

registerConfigCommand(program);
registerInstallCommand(program);
registerUploadCommand(program);
registerDownloadCommand(program);
registerLsCommand(program);
registerInfoCommand(program);
registerRmCommand(program);
registerMvCommand(program);
registerUrlCommand(program);
registerExistsCommand(program);

/**
 * 支持前置写法：mos --host X --profile dev ls /
 *
 * 根命令没有声明这些选项，这里在真正解析前把它们从 argv 中摘出来，
 * 转成同名环境变量，后续由 resolveConfig 统一读取。
 * 同时也让 `mos config show --profile dev` 之类的前置写法可用。
 */
/**
 * 支持前置写法：mos --host X --profile dev ls /
 *
 * 根命令没有声明这些选项，这里在真正解析前把它们从 argv 中摘出来，
 * 转成同名环境变量，后续由 resolveConfig 统一读取。
 */
const PRE_GLOBAL_OPTS: Record<string, string> = {
	'--host': 'MOS_HOST',
	'--bucket': 'MOS_BUCKET',
	'--secret-key': 'MOS_SECRET_KEY',
	'--open-id': 'MOS_OPEN_ID',
	'--profile': 'MOS_PROFILE',
};

/** 根命令自身的选项（不带值） */
const PRE_ROOT_FLAGS = ['--debug', '-v', '--version', '-h', '--help'];

function preParseGlobals(argv: string[]): string[] {
	const out: string[] = [];
	let i = 0;
	while (i < argv.length) {
		const arg = argv[i];
		if (arg === '--' || !arg.startsWith('-')) break; // 到达子命令

		const eq = arg.indexOf('=');
		const name = eq >= 0 ? arg.slice(0, eq) : arg;

		if (PRE_ROOT_FLAGS.includes(name)) {
			out.push(arg);
			i++;
			continue;
		}

		const envKey = PRE_GLOBAL_OPTS[name];
		if (envKey) {
			// 摘出全局选项并写入环境变量，让子命令沿用同一套配置解析
			if (eq >= 0) {
				process.env[envKey] = arg.slice(eq + 1);
				i++;
			} else if (i + 1 < argv.length && !argv[i + 1].startsWith('-')) {
				process.env[envKey] = argv[i + 1];
				i += 2;
			} else {
				out.push(arg);
				i++;
			}
			continue;
		}

		out.push(arg);
		i++;
	}
	return [...out, ...argv.slice(i)];
}

async function main(): Promise<void> {
	try {
		const argv = preParseGlobals(process.argv.slice(2));
		await program.parseAsync(['node', 'mos', ...argv]);
	} catch (e) {
		if (e instanceof MosError) {
			error(e.message);
		} else {
			error((e as Error).message ?? String(e));
		}
		process.exitCode = 1;
	}
}

void main();
