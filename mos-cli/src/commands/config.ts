/**
 * mos config —— 配置管理
 */

import { Command } from 'commander';
import {
	defaultConfigPath,
	loadConfigFile,
	MosProfile,
	normalizeHosts,
	resolveConfig,
	saveConfigFile,
} from '../core/config.js';
import { error, ok, warn } from '../util/ui.js';
import { addGlobalOptions, collectOverrides } from './context.js';
import { parseHosts } from '../core/hosts.js';

export function registerConfigCommand(program: Command): void {
	const config = program.command('config').description('配置管理（查看、设置、清空）');

	// mos config show
	addGlobalOptions(config.command('show').alias('list'))
		.description('查看当前生效的配置')
		.action((_opts: unknown, self: Command) => {
			const cfg = resolveConfig(collectOverrides(self));
			const lines: string[] = [];
			lines.push(`profile   : ${cfg.profile}`);
			if (cfg.hosts.length === 0) {
				lines.push(`host      : (未设置)`);
			} else if (cfg.hosts.length === 1) {
				lines.push(`host      : ${cfg.hosts[0].host}`);
			} else {
				lines.push(`host      : ${cfg.hosts.map((h) => h.host).join(', ')}（共 ${cfg.hosts.length} 个）`);
			}
			lines.push(`bucket    : ${cfg.bucket || '(未设置)'}`);
			lines.push(`secretKey : ${cfg.secretKey ? maskSecret(cfg.secretKey) : '(未设置)'}`);
			lines.push(`openId    : ${cfg.openId}`);
			lines.push(`配置文件  : ${defaultConfigPath()}`);
			console.log(lines.join('\n'));

			const missing: string[] = [];
			if (cfg.hosts.length === 0) missing.push('host');
			if (!cfg.bucket) missing.push('bucket');
			if (!cfg.secretKey) missing.push('secretKey');
			if (missing.length > 0) {
				warn(`配置不完整，缺少：${missing.join('、')}`);
			}
		});

	// mos config set —— 支持逐项设置，只更新传入的字段
	config
		.command('set')
		.description('设置配置项（保存到 ~/.mos/config.json，可逐项设置）')
		.option('--host <url>', 'mos 服务地址，例如 http://localhost:9700')
		.option('--bucket <name>', '桶名')
		.option('--secret-key <key>', '秘钥')
		.option('--open-id <id>', '开放平台 id', (v) => Number(v))
		.option('--profile <name>', '配置档案名', 'default')
		.action((opts: {
			host?: string;
			bucket?: string;
			secretKey?: string;
			openId?: number;
			profile: string;
		}) => {
			// 至少要设置一项
			if (opts.host === undefined && opts.bucket === undefined && opts.secretKey === undefined && opts.openId === undefined) {
				error('请至少指定一项，例如：mos config set --host http://localhost:9700');
				console.log('可用项：--host / --bucket / --secret-key / --open-id');
				process.exitCode = 1;
				return;
			}

			const file = loadConfigFile();
			file.profiles = file.profiles ?? {};

			// 基于已有配置合并，未传入的字段保持不变
			const existing: MosProfile = file.profiles[opts.profile] ?? {
				host: '',
				bucket: '',
				secretKey: '',
				openId: 0,
			};
			const profile: MosProfile = {
				host: opts.host !== undefined ? normalizeHosts(opts.host) : existing.host,
				bucket: opts.bucket ?? existing.bucket,
				secretKey: opts.secretKey ?? existing.secretKey,
				openId: opts.openId !== undefined ? opts.openId : (existing.openId ?? 0),
			};

			const isNew = file.profiles[opts.profile] === undefined;
			file.profiles[opts.profile] = profile;
			// 只在该档案是新建、且当前还没有默认档案时才设为默认，
			// 避免 `config set --profile dev` 悄悄改掉 default 档案
			if (!file.defaultProfile || file.defaultProfile === opts.profile) {
				file.defaultProfile = opts.profile;
			}

			const saved = saveConfigFile(file);
			ok(`配置已${isNew ? '创建' : '更新'}：${saved}`);

			// 只提示本次实际修改的项
			const changed: string[] = [];
			if (opts.host !== undefined) changed.push('host');
			if (opts.bucket !== undefined) changed.push('bucket');
			if (opts.secretKey !== undefined) changed.push('secretKey');
			if (opts.openId !== undefined) changed.push('openId');
			console.log(`  profile   : ${opts.profile}（本次修改：${changed.join('、')}）`);
			console.log(`  host      : ${profile.host || '(未设置)'}`);
			console.log(`  bucket    : ${profile.bucket || '(未设置)'}`);
			console.log(`  secretKey : ${profile.secretKey ? maskSecret(profile.secretKey) : '(未设置)'}`);
			console.log(`  openId    : ${profile.openId}`);
		});

	// mos config use —— 切换默认档案
	config
		.command('use')
		.description('设置默认使用的配置档案')
		.argument('<profile>', '档案名')
		.action((profile: string) => {
			const file = loadConfigFile();
			if (!file.profiles?.[profile]) {
				error(`档案不存在：${profile}`);
				process.exitCode = 1;
				return;
			}
			file.defaultProfile = profile;
			const saved = saveConfigFile(file);
			ok(`默认档案已切换为：${profile}`);
			const p = file.profiles[profile];
			console.log(`  host      : ${p.host || '(未设置)'}`);
			console.log(`  bucket    : ${p.bucket || '(未设置)'}`);
		});

	// mos config unset / rm
	config.command('unset')
		.argument('<key>', 'host | bucket | secretKey | openId')
		.option('--profile <name>', '配置档案名', 'default')
		.action((key: string, opts: { profile: string }) => {
			const file = loadConfigFile();
			const profile = file.profiles?.[opts.profile];
			if (!profile) {
				error(`档案不存在：${opts.profile}`);
				process.exitCode = 1;
				return;
			}
			if (!(key in profile)) {
				error(`未知配置项：${key}`);
				process.exitCode = 1;
				return;
			}
			(profile as unknown as Record<string, unknown>)[key] = key === 'openId' ? 0 : '';
			const saved = saveConfigFile(file);
			ok(`已清空 ${key}（${saved}）`);
		});

	// mos config profiles
	config.command('profiles')
		.description('列出所有配置档案')
		.action(() => {
			const file = loadConfigFile();
			const names = Object.keys(file.profiles ?? {});
			if (names.length === 0) {
				warn('还没有任何配置档案');
				return;
			}
			for (const name of names) {
				const p = file.profiles![name];
				const mark = name === file.defaultProfile ? '*' : ' ';
				const hosts = parseHosts(p.host);
				const hostStr = hosts.length === 0 ? '(未设置)' : hosts.length === 1 ? hosts[0].host : `${hosts[0].host} 等 ${hosts.length} 个`;
				console.log(`${mark} ${name.padEnd(20)} ${hostStr}/${p.bucket}`);
			}
		});
}

function maskSecret(secret: string): string {
	if (secret.length <= 8) return '****';
	return `${secret.slice(0, 4)}****${secret.slice(-4)}`;
}
