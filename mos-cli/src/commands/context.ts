/**
 * 全局选项解析：把 commander 的 opts + 环境变量 + 配置合并成最终配置。
 */

import { Command } from 'commander';
import { assertUsable, ConfigOverrides, resolveConfig } from '../core/config.js';
import { MosClient } from '../core/client.js';

export interface GlobalFlags {
	host?: string;
	bucket?: string;
	secretKey?: string;
	openId?: number;
	profile?: string;
	debug?: boolean;
}

/**
 * 给子命令注册全局选项。
 *
 * 之所以要在每个子命令上重复声明，是因为 mos config set --host 这类
 * 子命令选项与全局选项同名；若开启 enablePositionalOptions，
 * 全局选项将无法在子命令后使用。
 * 这里为每个子命令都挂一份，既保证 `mos ls --profile dev` 可用，
 * 又能让子命令自身的同名选项正确覆盖全局值。
 */
export function addGlobalOptions(cmd: Command): Command {
	return cmd
		.option('--host <url>', 'mos 服务地址（覆盖配置文件）')
		.option('--bucket <name>', '桶名（覆盖配置文件）')
		.option('--secret-key <key>', '秘钥（覆盖配置文件）')
		.option('--open-id <id>', '开放平台 id（覆盖配置文件）', (v) => parseInt(v, 10))
		.option('--profile <name>', '使用的配置档案（默认 default）');
}

/**
 * 从任意 command 上收集全局选项。
 *
 * 注意：使用 optsWithGlobals 会把祖先命令的选项合并进来，
 * 但子命令自身的同名选项（如 config set --host）优先级更高，符合预期。
 */
export function collectOverrides(cmd: Command): ConfigOverrides {
	const g = (cmd.optsWithGlobals?.() ?? cmd.opts()) as GlobalFlags;
	const overrides: ConfigOverrides = {};
	if (g.host) overrides.host = g.host;
	if (g.bucket) overrides.bucket = g.bucket;
	if (g.secretKey) overrides.secretKey = g.secretKey;
	if (g.openId !== undefined && g.openId !== null) overrides.openId = Number(g.openId);
	// 显式传了 --profile 才算覆盖；否则用配置里的默认档案
	if (g.profile !== undefined && g.profile !== null) {
		overrides.profile = g.profile;
	}
	return overrides;
}

/**
 * 创建已校验的客户端。
 *
 * @param cmd 实际执行的子命令（不是根 program），
 *            这样 optsWithGlobals 才能拿到全局选项。
 */
export function createClient(cmd: Command): MosClient {
	const cfg = resolveConfig(collectOverrides(cmd));
	assertUsable(cfg);

	if ((cmd.optsWithGlobals?.() as GlobalFlags).debug) {
		process.env.MOS_DEBUG = '1';
	}
	return new MosClient(cfg);
}
