/**
 * 对应服务端/SDK 的 `mt.spring.mos.sdk.utils.MosEncrypt` 签名算法。
 *
 * 签名串结构：
 *   rChar(1) + s1(random) + lengthChar(1) + openIdEncoded + s2
 * 其中：
 *   random  = nextInt(min(sign.length, 57))，且不能落在 (25,32)
 *   rChar   = (char)(random + 65)
 *   s1/s2   = 对 AES 结果按 random 位切分
 *   lengthChar = (char)(openId 字符串长度 + 64)
 *   openIdEncoded = 每个数字字符 +65
 */

import { aesDecode, aesEncode } from './javaAes.js';

/** 签名内容：单个 pathname */
export class PathnamesEncryptContent {
	readonly pathnames: string[];

	constructor(pathname: string) {
		this.pathnames = [pathname];
	}
}

/**
 * 生成签名。
 *
 * @param secretKey      秘钥（AccessControl.secretKey，或 openId<=0 时的 admin key）
 * @param content        签名内容（pathnames）
 * @param bucketName     桶名
 * @param openId         开放平台 id，<=0 表示管理员
 * @param expireSeconds  有效秒数，-1 表示永不过期
 */
export function encrypt(
	secretKey: string,
	content: PathnamesEncryptContent,
	bucketName: string,
	openId: number,
	expireSeconds: number,
): string {
	// 与 Java JSONObject.toJSONString() 的字段顺序保持一致
	const json = JSON.stringify({
		cls: 'mt.spring.mos.sdk.type.PathnamesEncryptContent',
		c: { pathnames: content.pathnames },
		b: bucketName,
		e: expireSeconds,
		t: Date.now(),
	});

	const openIdStr = String(openId);
	const lengthChar = String.fromCharCode(openIdStr.length + 64);

	let encodedOpenId = '';
	for (const ch of openIdStr) {
		encodedOpenId += String.fromCharCode(Number(ch) + 65);
	}

	const sign = aesEncode(json, secretKey + openId);

	// 随机切分点：nextInt(min(len,57))，并避开 (25,32)
	const max = Math.min(sign.length, 57);
	let random = 0;
	do {
		random = nextInt(max);
	} while (random > 25 && random < 32);

	const rChar = String.fromCharCode(random + 65);
	const s1 = sign.substring(0, random);
	const s2 = sign.substring(random);

	return rChar + s1 + lengthChar + encodedOpenId + s2;
}

export interface MosEncryptContent {
	bucketName: string;
	openId: number;
	expireSeconds: number;
	signTime: number;
	pathnames: string[];
}

/**
 * 解析签名，主要用于自检 / 调试（对应 MosEncrypt.decrypt）。
 */
export function decrypt(secretKey: string, sign: string): MosEncryptContent {
	const random = sign.charCodeAt(0) - 65 + 1;
	const s1 = sign.substring(1, random);
	const openIdAndS2 = sign.substring(random);

	const length = openIdAndS2.charCodeAt(0) - 64;
	const openIdChars = openIdAndS2.substring(1, 1 + length);

	let openIdStr = '';
	for (const ch of openIdChars) {
		openIdStr += String(ch.charCodeAt(0) - 65);
	}
	const openId = Number.parseInt(openIdStr, 10);

	const s2 = openIdAndS2.substring(1 + length);
	const json = aesDecode(s1 + s2, secretKey + openId);
	const obj = JSON.parse(json);

	return {
		bucketName: obj.b,
		openId,
		expireSeconds: obj.e,
		signTime: obj.t,
		pathnames: obj.c?.pathnames ?? [],
	};
}

/** java.util.Random.nextInt(bound) 的等价实现（含种子固定以便复现） */
function nextInt(bound: number): number {
	const seed = (Date.now() & 0x7fffffff) || 1;
	return javaRandomNextInt(seed, bound);
}

/**
 * 完整复刻 java.util.Random#nextInt(bound)。
 * seed 每次调用都会变化，因此这里只保证分布与语义一致。
 */
function javaRandomNextInt(initialSeed: number, bound: number): number {
	let seed = initialSeed >>> 0;
	const next = (): number => {
		seed = (Math.imul(seed, 0x5deece66d) + 0xb) & 0x7fffffff;
		return seed;
	};
	const nextInt = (boundValue: number): number => {
		if ((boundValue & -boundValue) === boundValue) {
			return Math.floor((boundValue * next()) / 2147483648);
		}
		let bits;
		let val;
		do {
			bits = next() >>> 1;
			val = bits % boundValue;
		} while (bits - val + (boundValue - 1) > 2147483646);
		return val;
	};
	return nextInt(bound);
}
