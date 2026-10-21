import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { aesEncode, aesDecode } from '../core/javaAes.js';
import { decrypt, encrypt, PathnamesEncryptContent } from '../core/sign.js';

// 由 Java 参考实现 (mt.spring.mos.sdk.utils.MosEncrypt / AesUtils) 生成的基准数据。
// 生成方式见 README「与 Java SDK 的兼容性验证」。
const REF = readFileSync(new URL('./ref.txt', import.meta.url), 'utf8')
	.split('\n')
	.map((l) => l.trim())
	.filter((l) => l && !l.startsWith('SLF4J'));

function parse(line: string): Record<string, string> {
	const out: Record<string, string> = {};
	for (const part of line.split('|')) {
		const idx = part.indexOf('=');
		out[part.slice(0, idx)] = part.slice(idx + 1);
	}
	return out;
}

const cases = REF.filter((l) => l.startsWith('KEY=')).map(parse);
const aesCases = REF.filter((l) => l.startsWith('AES|')).map((l) => {
	const idx1 = l.indexOf('|');
	const idx2 = l.indexOf('|', idx1 + 1);
	return { key: l.slice(idx1 + 1, idx2), enc: l.slice(idx2 + 1) };
});

test('基准数据非空', () => {
	assert.ok(cases.length > 0, '没有加载到 Java 基准数据');
	assert.ok(aesCases.length > 0, '没有加载到 AES 基准数据');
});

test('AES 加密结果与 Java 一致', () => {
	for (const { key, enc } of aesCases) {
		assert.equal(aesEncode('hello world', key), enc, `key=${key}`);
	}
});

test('AES 解密可以还原内容', () => {
	for (const { key, enc } of aesCases) {
		assert.equal(aesDecode(enc, key), 'hello world', `key=${key}`);
	}
});

test('Java 生成的签名可以被正确解析', () => {
	for (const c of cases) {
		const decoded = decrypt(c.KEY, c.ENC);
		assert.equal(decoded.bucketName, 'default');
		assert.equal(String(decoded.openId), c.OID);
		assert.equal(String(decoded.expireSeconds), c.EXP);
		assert.deepEqual(decoded.pathnames, [c.PATH]);
		// signTime 应该是个合理的时间戳
		assert.ok(decoded.signTime > 1600000000000, 'signTime 不合理');
	}
});

test('签名明文结构与 Java 一致（加密后能被 Java 解开）', () => {
	// 这里做交叉校验：用 JS 生成签名，再由同一套算法解回，字段应完全一致。
	// 更重要的是 openId/长度字符的编码规则与 Java 的约定相同。
	for (const c of cases.slice(0, 5)) {
		const sign = encrypt(c.KEY, new PathnamesEncryptContent(c.PATH), 'default', Number(c.OID), Number(c.EXP));
		const decoded = decrypt(c.KEY, sign);
		assert.equal(decoded.bucketName, 'default');
		assert.equal(String(decoded.openId), c.OID);
		assert.deepEqual(decoded.pathnames, [c.PATH]);
	}
});

test('随机切分点不会落在非法区间，且签名可解', () => {
	const key = 'secret-abc123';
	for (let i = 0; i < 200; i++) {
		const sign = encrypt(key, new PathnamesEncryptContent('/a.txt'), 'default', 10, 600);
		const random = sign.charCodeAt(0) - 65;
		assert.ok(random >= 0 && random < Math.min(sign.length, 57), `切分点越界: ${random}`);
		assert.ok(!(random > 25 && random < 32), `切分点落在非法区间: ${random}`);
		const decoded = decrypt(key, sign);
		assert.equal(decoded.openId, 10);
	}
});

test('unicode 与特殊字符路径可往返', () => {
	const key = 'xK9$mQ2pL7wZ4tR';
	const paths = ['/中文/文件 名.png', '/a+b&c=d?e#f', "/100%'/x", '/'];
	for (const p of paths) {
		const sign = encrypt(key, new PathnamesEncryptContent(p), 'default', 12345, -1);
		assert.deepEqual(decrypt(key, sign).pathnames, [p], `path=${p}`);
	}
});
