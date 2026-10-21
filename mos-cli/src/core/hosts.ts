/**
 * 多地址（hosts）支持，对应 Java SDK 的 HostChooseUtils。
 *
 * 配置里的 host 可以是单个地址，也可以是逗号分隔的多个地址，
 * 每个地址还可带权重后缀，例如：
 *
 *   http://10.0.0.1:4100,http://10.0.0.2:4100
 *   http://192.168.0.174:4100(50),http://192.168.0.2:4100(0)
 *
 * 规则与 Java 端保持一致：
 *   - 不以 http 开头的地址自动补 http://
 *   - (N) 为权重，缺省 100；权重 0 表示不参与选择
 *   - 可用性按 checkUri 探测（带缓存），只有可用的地址参与选择
 *   - 按权重随机挑选
 */

import * as http from 'node:http';
import * as https from 'node:https';
import { URL } from 'node:url';

/** 健康检查默认地址，与 MosConfig.getHost() 使用的 /index.html 一致 */
export const DEFAULT_CHECK_URI = '/index.html';

/** 健康检查结果缓存时间（毫秒） */
const DEFAULT_CACHE_TTL = 10_000;

export interface ParsedHost {
	/** 去掉权重后缀的真实地址 */
	host: string;
	/** 权重，缺省 100 */
	weight: number;
}

/**
 * 解析单个 host 条目。
 *
 * 协议可省略，省略时默认 http://；显式的 https:// 会被保留。
 * 支持权重后缀 (N)。
 *
 *   192.168.0.1:4100            -> http://192.168.0.1:4100
 *   mos.example.com             -> http://mos.example.com
 *   https://a.com(50)           -> https://a.com, weight 50
 *   192.168.0.1:4100/           -> http://192.168.0.1:4100
 */
export function parseHost(entry: string): ParsedHost {
	let host = entry.trim();
	if (!host) {
		return { host: '', weight: 100 };
	}
	// 已有明确协议则保留，否则默认补 http://
	if (!/^https?:\/\//i.test(host)) {
		host = 'http://' + host;
	}

	let weight = 100;
	// 取最后一个 "(" 之后的内容作为权重
	const index = host.lastIndexOf('(');
	if (index !== -1 && host.endsWith(')')) {
		const weightPart = host.slice(index + 1);
		const parsed = parseInt(weightPart.slice(0, -1), 10);
		if (Number.isFinite(parsed)) {
			weight = parsed;
			host = host.slice(0, index).trim();
		}
	}
	// 去掉结尾斜杠
	host = host.replace(/\/+$/, '');
	return { host, weight };
}

/**
 * 把配置里的 host 字段拆成多个地址。
 * 支持逗号、分号、换行分隔。
 */
export function parseHosts(value: string | string[] | undefined): ParsedHost[] {
	if (!value) return [];

	const rawList = Array.isArray(value)
		? value
		: String(value)
				.split(/[,;\n]/)
				.map((s) => s.trim())
				.filter(Boolean);

	return rawList.map(parseHost).filter((h) => h.host !== '');
}

// ---------------------------------------------------------------- 健康检查

interface HealthState {
	available: boolean;
	checkedAt: number;
}

const healthCache = new Map<string, HealthState>();

/** 探测单个地址是否可用 */
export function probeHost(host: string, checkUri: string, timeoutMs = 5000): Promise<boolean> {
	return new Promise((resolve) => {
		let parsed: URL;
		try {
			parsed = new URL(host + checkUri);
		} catch {
			resolve(false);
			return;
		}

		const lib = parsed.protocol === 'https:' ? https : http;
		const req = lib.request(
			parsed,
			{ method: 'GET', rejectUnauthorized: false, headers: { 'User-Agent': 'mos-cli' } },
			(res) => {
				const ok = res.statusCode !== undefined && res.statusCode >= 200 && res.statusCode < 400;
				res.resume();
				resolve(ok);
			},
		);
		req.setTimeout(timeoutMs, () => {
			req.destroy();
			resolve(false);
		});
		req.on('error', () => resolve(false));
		req.end();
	});
}

/** 带缓存的可用性检查 */
export async function isHostAvailable(
	host: string,
	checkUri: string,
	ttl = DEFAULT_CACHE_TTL,
	timeoutMs = 5000,
): Promise<boolean> {
	const key = host + checkUri;
	const cached = healthCache.get(key);
	const now = Date.now();
	if (cached && now - cached.checkedAt < ttl) {
		return cached.available;
	}

	const available = await probeHost(host, checkUri, timeoutMs);
	healthCache.set(key, { available, checkedAt: now });
	return available;
}

/** 清空健康检查缓存（测试用） */
export function clearHealthCache(): void {
	healthCache.clear();
}

export interface PickHostOptions {
	/** 健康检查路径，空字符串表示不检查 */
	checkUri?: string | null;
	/** 缓存时间 */
	ttl?: number;
	/** 探测超时 */
	timeoutMs?: number;
	/** 随机源，便于测试注入 */
	random?: () => number;
}

/**
 * 按权重从可用地址中随机选择一个。
 *
 * 与 Java 的 getAvailableHostByWeight 行为一致：
 *   - 过滤掉健康检查未通过的地址
 *   - 权重全为 0 时统一按 100 处理（即仍可被选中）
 *   - 只要存在权重 > 0 的地址，权重为 0 的地址就不会被选中
 *   - 按权重随机排序后取第一个
 */
export async function pickHost(
	hosts: ParsedHost[],
	options: PickHostOptions = {},
): Promise<string | null> {
	const { checkUri = DEFAULT_CHECK_URI, ttl = DEFAULT_CACHE_TTL, timeoutMs = 5000, random = Math.random } = options;

	const usable: ParsedHost[] = [];
	for (const h of hosts) {
		if (h.weight < 0) continue; // 负权重视为无效
		if (!checkUri) {
			usable.push(h);
			continue;
		}
		if (await isHostAvailable(h.host, checkUri, ttl, timeoutMs)) {
			usable.push(h);
		}
	}

	if (usable.length === 0) return null;
	if (usable.length === 1) return usable[0].host;

	// 存在权重 > 0 的地址时，权重为 0 的不参与竞争
	const hasPositive = usable.some((h) => h.weight > 0);
	const candidates = hasPositive ? usable.filter((h) => h.weight > 0) : usable;

	// 按权重随机打分，取最高分；等价于 Java 的随机排序
	let best = candidates[0];
	let bestScore = random() * Math.max(best.weight, 1);
	for (let i = 1; i < candidates.length; i++) {
		const score = random() * Math.max(candidates[i].weight, 1);
		if (score > bestScore) {
			best = candidates[i];
			bestScore = score;
		}
	}
	return best.host;
}

/**
 * 按顺序回退：依次尝试每个可用地址，返回第一个调用成功的。
 * 用于上传/下载这类必须成功的操作，避免单次随机选到坏节点就直接失败。
 */
export async function forEachHost<T>(
	hosts: ParsedHost[],
	fn: (host: string) => Promise<T>,
	options: PickHostOptions = {},
): Promise<T> {
	const { checkUri = null, ttl = DEFAULT_CACHE_TTL } = options;

	// 先按权重排序，优先尝试高权重的
	let ordered = hosts;
	if (hosts.length > 1) {
		ordered = [...hosts].sort((a, b) => b.weight - a.weight);
	}

	const errors: string[] = [];
	for (const h of ordered) {
		if (checkUri && !(await isHostAvailable(h.host, checkUri, ttl))) {
			errors.push(`${h.host}: 健康检查未通过`);
			continue;
		}
		try {
			return await fn(h.host);
		} catch (e) {
			errors.push(`${h.host}: ${(e as Error).message}`);
		}
	}

	throw new Error(
		`所有地址都不可用：\n  ${errors.join('\n  ')}` +
			(ordered.length === 1 ? `\n请检查 mos config set --host <地址> 是否正确。` : ''),
	);
}
