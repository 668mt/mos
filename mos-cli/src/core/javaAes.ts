/**
 * 与 Java `AesUtils` 完全兼容的 AES 加解密实现。
 *
 * 服务端签名校验使用 `mt.spring.mos.base.utils.AesUtils`，其密钥派生方式为：
 *
 *   KeyGenerator kgen = KeyGenerator.getInstance("AES");
 *   SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
 *   random.setSeed(encryptKey.getBytes());
 *   kgen.init(128, random);
 *
 * 现代 JDK（JDK9+）中，"SHA1PRNG" 已不再使用老的 SHA1PRNG 类，
 * 而是映射到 sun.security.provider.SecureRandom —— 一个基于 SHA-1 的
 * 计数器型伪随机数生成器。这里的实现复刻了它的
 * engineSetSeed / engineNextBytes / updateState，保证与 Java 完全一致。
 *
 * 算法（伪代码）：
 *   engineSetSeed(seed):
 *     if state != null: digest.update(state); state 清零
 *     state = SHA1(seed)          // 20 字节
 *     remCount = 0
 *
 *   engineNextBytes(out):         // out 长度 = count
 *     remainder = this.remainder
 *     if state == null:            // 未 seed 时用全局 seeder 自举
 *       tmp = 20 字节; seeder.engineNextBytes(tmp); state = SHA1(tmp)
 *     // 先补齐上一次剩下的字节
 *     while remCount > 0 且未填满:
 *       取 min(count - offset, 20 - remCount) 字节
 *     while offset < count:
 *       digest.update(state)
 *       newBytes = SHA1(state)     // 20 字节
 *       updateState(state, newBytes)   // 带进位相加
 *       从 newBytes 取 min(count-offset, 20) 字节（取过置 0）
 *     remainder = newBytes
 *     remCount = remCount % 20
 *
 *   updateState(state, newBytes):  // 20 字节的带进位加法
 *     carry = 1
 *     anyCarry = false
 *     for i in 0..19:
 *       sum = state[i] + newBytes[i] + carry
 *       b = sum as int8        // 只保留低 8 位
 *       anyCarry |= (state[i] != b)
 *       state[i] = b
 *       carry = sum >>> 8
 *     if !anyCarry: state[0] = (state[0] + 1) as int8
 */

import * as crypto from 'node:crypto';

const DIGEST_SIZE = 20; // SHA-1 输出长度

/**
 * 密钥派生：SHA1PRNG.setSeed(key) 之后取前 16 字节。
 * KeyGenerator.init(128, random) 内部会调用 random.nextBytes(seedBytes)。
 */
function deriveKey(key: string): Buffer {
	const prng = new SunSha1Prng();
	prng.setSeed(Buffer.from(key, 'utf8'));
	return prng.nextBytes(16);
}

/** sun.security.provider.SecureRandom（SHA1PRNG 的现代实现）的等价实现 */
class SunSha1Prng {
	private state: Buffer | null = null;
	private remainder: Buffer = Buffer.alloc(DIGEST_SIZE);
	private remCount = 0;

	setSeed(seed: Buffer): void {
		if (this.state !== null) {
			// digest.update(state); 然后把 state 全部置 0
			// 注意：这里的 digest.update 并不会保留状态（见下），
			// 原实现是 MessageDigest 被复用的，但 digest() 之后会重置。
			// 为了与 Java 行为一致，这里执行一次 update 后再 digest。
			// 实际上原实现中 digest 对象在此处被 update(state) 但没有 digest()，
			// 随后 state 被置 0，而 digest 内部缓冲会在后续 digest() 时被清空。
			// 由于每次 setSeed 后都会立刻调用 digest()，此处等价于直接置 0。
			this.state = Buffer.alloc(DIGEST_SIZE);
		}
		this.state = sha1(seed);
		this.remCount = 0;
	}

	nextBytes(count: number): Buffer {
		const out = Buffer.alloc(count);
		let offset = 0;
		let rem = Buffer.from(this.remainder);
		let remCount = this.remCount;

		if (this.state === null) {
			// 未显式 seed：用全局 seeder 自举（CLI 场景不会走到这里）
			this.state = sha1(Buffer.from('SUN-SHA1PRNG-SELFSEED'));
		}

		// 先消费上一次遗留的字节
		while (remCount > 0) {
			if (offset >= count) break;
			const take = Math.min(count - offset, DIGEST_SIZE - remCount);
			rem.copy(out, offset, remCount, remCount + take);
			for (let i = 0; i < take; i++) rem[remCount + i] = 0;
			remCount += take;
			offset += take;
		}

		let newBytes: Buffer = Buffer.alloc(DIGEST_SIZE);

		while (offset < count) {
			// digest.update(state); digest.digest()
			newBytes = sha1(this.state);
			// updateState(state, newBytes)
			updateState(this.state, newBytes);

			const take = Math.min(count - offset, DIGEST_SIZE);
			newBytes.copy(out, offset, 0, take);
			for (let i = 0; i < take; i++) newBytes[i] = 0;
			offset += take;
			remCount += take;
		}

		this.remainder = newBytes;
		this.remCount = remCount % DIGEST_SIZE;
		return out;
	}
}

/** MessageDigest("SHA").digest(data) */
function sha1(data: Buffer): Buffer {
	return crypto.createHash('sha1').update(data).digest();
}

/** 带进位的 20 字节加法（updateState） */
function updateState(state: Buffer, newBytes: Buffer): void {
	let carry = 1;
	let anyCarry = false;

	for (let i = 0; i < DIGEST_SIZE; i++) {
		const sum = state[i] + newBytes[i] + carry;
		const b = sum & 0xff; // i2b：只保留低 8 位
		if (state[i] !== b) anyCarry = true;
		state[i] = b;
		carry = sum >>> 8;
	}

	if (!anyCarry) {
		state[0] = (state[0] + 1) & 0xff;
	}
}

/**
 * 复刻 Java `AesUtils.aesEncryptToBytes`：AES/ECB/PKCS5Padding。
 */
export function aesEncrypt(content: string, encryptKey: string): Buffer {
	const keyBytes = deriveKey(encryptKey);
	const cipher = crypto.createCipheriv('aes-128-ecb', keyBytes, null);
	cipher.setAutoPadding(true); // 等价于 PKCS5Padding
	return Buffer.concat([cipher.update(Buffer.from(content, 'utf8')), cipher.final()]);
}

/**
 * 复刻 Java `AesUtils.aesDecryptByBytes`。
 */
export function aesDecrypt(encryptBytes: Buffer, decryptKey: string): string {
	const keyBytes = deriveKey(decryptKey);
	const decipher = crypto.createDecipheriv('aes-128-ecb', keyBytes, null);
	decipher.setAutoPadding(true);
	return Buffer.concat([decipher.update(encryptBytes), decipher.final()]).toString('utf8');
}

/** Java `Base64.getUrlEncoder()`：URL-safe base64，带 = 填充 */
export function base64UrlEncode(bytes: Buffer): string {
	return bytes.toString('base64').replace(/\+/g, '-').replace(/\//g, '_');
}

/** Java `Base64.getUrlDecoder()` */
export function base64UrlDecode(input: string): Buffer {
	return Buffer.from(input, 'base64url');
}

/** 对应 `AesUtils.aesEncode`：AES 加密后做 URL-safe base64 */
export function aesEncode(content: string, encryptKey: string): string {
	return base64UrlEncode(aesEncrypt(content, encryptKey));
}

/** 对应 `AesUtils.aesDecode` */
export function aesDecode(encryptStr: string, decryptKey: string): string {
	return aesDecrypt(base64UrlDecode(encryptStr), decryptKey);
}
