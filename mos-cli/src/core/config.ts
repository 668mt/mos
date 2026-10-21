/**
 * CLI 配置管理。
 *
 * 配置文件位置优先级：
 *   1. --config 指定的路径
 *   2. 环境变量 MOS_CONFIG
 *   3. ~/.mos/config.json
 *
 * 凭据优先级：
 *   命令行参数 > 环境变量 > 配置文件
 */

import * as fs from 'node:fs';
import * as path from 'node:path';
import * as os from 'node:os';
import { parseHosts, ParsedHost } from './hosts.js';

export interface MosProfile {
	/** mos 服务地址；支持多个，逗号分隔，可带权重后缀，如 http://a:4100(50),http://b:4100 */
	host: string;
	/** 桶名 */
	bucket: string;
	/** 秘钥（AccessControl.secretKey） */
	secretKey: string;
	/** 开放平台 id，<=0 表示管理员 */
	openId: number;
}

export interface MosConfigFile {
	defaultProfile?: string;
	profiles?: Record<string, MosProfile>;
}

export function defaultConfigPath(): string {
	return path.join(os.homedir(), '.mos', 'config.json');
}

export function emptyConfig(): MosConfigFile {
	return { profiles: {} };
}

export function loadConfigFile(configPath?: string): MosConfigFile {
	const file = configPath || process.env.MOS_CONFIG || defaultConfigPath();
	if (!fs.existsSync(file)) {
		return emptyConfig();
	}
	try {
		const raw = fs.readFileSync(file, 'utf8');
		const parsed = JSON.parse(raw) as MosConfigFile;
		return {
			defaultProfile: parsed.defaultProfile,
			profiles: parsed.profiles ?? {},
		};
	} catch (e) {
		throw new Error(`配置文件解析失败：${(e as Error).message}`);
	}
}

export function saveConfigFile(config: MosConfigFile, configPath?: string): string {
	const file = configPath || process.env.MOS_CONFIG || defaultConfigPath();
	fs.mkdirSync(path.dirname(file), { recursive: true });
	fs.writeFileSync(file, JSON.stringify(config, null, 2) + '\n', 'utf8');
	return file;
}

/** 收集生效的配置来源，供 `mos config show` 展示 */
export interface ResolvedConfig {
	profile: string;
	host: string;
	/** 解析后的地址列表（已去掉权重后缀） */
	hosts: ParsedHost[];
	bucket: string;
	secretKey: string;
	openId: number;
	sources: Record<string, string>;
}

export interface ConfigOverrides {
	host?: string;
	bucket?: string;
	secretKey?: string;
	openId?: number;
	profile?: string;
}

/**
 * 解析最终生效的配置。
 * 优先级：命令行参数 > 环境变量 > 配置文件
 */
export function resolveConfig(overrides: ConfigOverrides = {}): ResolvedConfig {
	const configPath = process.env.MOS_CONFIG || defaultConfigPath();
	const file = loadConfigFile(overrides.profile ? undefined : undefined);

	const profileName = overrides.profile || process.env.MOS_PROFILE || file.defaultProfile || 'default';
	const base: MosProfile = file.profiles?.[profileName] ?? { host: '', bucket: '', secretKey: '', openId: 0 };

	const sources: Record<string, string> = {};
	const setIf = (key: string, fromEnv: string | undefined, fromArg: string | undefined, fromFile: string | undefined) => {
		if (fromArg !== undefined && fromArg !== '') {
			sources[key] = '命令行参数';
		} else if (fromEnv !== undefined && fromEnv !== '') {
			sources[key] = '环境变量';
		} else if (fromFile !== undefined && fromFile !== '') {
			sources[key] = `配置文件(${profileName})`;
		}
	};

	const host = overrides.host || process.env.MOS_HOST || base.host;
	const bucket = overrides.bucket || process.env.MOS_BUCKET || base.bucket;
	const secretKey = overrides.secretKey || process.env.MOS_SECRET_KEY || base.secretKey;

	let openId: number;
	if (overrides.openId !== undefined) {
		openId = overrides.openId;
	} else if (process.env.MOS_OPEN_ID) {
		openId = Number(process.env.MOS_OPEN_ID);
	} else {
		openId = base.openId ?? 0;
	}

	setIf('host', process.env.MOS_HOST, overrides.host, base.host);
	setIf('bucket', process.env.MOS_BUCKET, overrides.bucket, base.bucket);
	setIf('secretKey', process.env.MOS_SECRET_KEY, overrides.secretKey, base.secretKey);
	setIf(
		'openId',
		process.env.MOS_OPEN_ID,
		overrides.openId !== undefined ? String(overrides.openId) : undefined,
		base.openId !== undefined && base.openId !== 0 ? String(base.openId) : undefined,
	);

	// 记录配置文件路径来源
	if (fs.existsSync(configPath)) {
		sources['configFile'] = configPath;
	}

	const hosts = parseHosts(host);

	return { profile: profileName, host, hosts, bucket, secretKey, openId, sources };
}

/** 校验配置是否完整 */
export function assertUsable(cfg: ResolvedConfig): void {
	const missing: string[] = [];
	if (cfg.hosts.length === 0) missing.push('host（可用 --host 或 MOS_HOST 设置）');
	if (!cfg.bucket) missing.push('bucket（可用 --bucket 或 MOS_BUCKET 设置）');
	if (!cfg.secretKey) missing.push('secretKey（可用 --secret-key 或 MOS_SECRET_KEY 设置）');
	if (missing.length > 0) {
		throw new Error(
			`配置不完整，缺少：${missing.join('、')}。\n` +
				`请先执行：mos config set --host <地址> --bucket <桶名> --secret-key <秘钥>`,
		);
	}
	// 每个地址都要能被 URL 解析（协议已在 parseHost 补全）
	for (const h of cfg.hosts) {
		try {
			new URL(h.host);
		} catch {
			throw new Error(`host 格式不正确：${h.host}`);
		}
	}
	if (!Number.isFinite(cfg.openId)) {
		throw new Error(`openId 必须是数字：${cfg.openId}`);
	}
}

/** 去掉 host 结尾的斜杠 */
export function normalizeHost(host: string): string {
	return host.replace(/\/+$/, '');
}

/**
 * 规范化 host 配置：拆分多个地址、补全协议、去掉结尾斜杠，
 * 再用逗号拼回一个字符串保存。
 *
 *   192.168.0.1:4100, 192.168.0.2:4100  ->  http://192.168.0.1:4100,http://192.168.0.2:4100
 */
export function normalizeHosts(value: string): string {
	return parseHosts(value)
		.map((h) => h.host)
		.join(',');
}
