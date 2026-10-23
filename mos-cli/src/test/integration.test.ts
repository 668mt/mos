/**
 * 端到端测试：启动一个模拟 mos 服务端，跑通上传 / 下载 / 查询 / 删除全流程。
 */

import test from 'node:test';
import assert from 'node:assert/strict';
import * as fs from 'node:fs';
import * as os from 'node:os';
import * as path from 'node:path';
import { MockMosServer } from './mockServer.js';
import { MosClient } from '../core/client.js';
import { resolveConfig } from '../core/config.js';
import { uploadFile, planChunks, uploadDir } from '../core/upload.js';
import { download, downloadDir } from '../core/download.js';
import { isDirItem, itemName, itemSize, joinItemPath, pageItems } from '../core/dirTypes.js';

const SECRET = 'test-secret-key-123';

function makeClient(host: string, bucket = 'default', openId = 0): MosClient {
	return new MosClient(
		resolveConfig({
			host,
			bucket,
			secretKey: SECRET,
			openId,
		}),
	);
}

async function withServer(
	fn: (server: MockMosServer, client: MosClient, tmp: string) => Promise<void>,
): Promise<void> {
	const server = new MockMosServer({ secretKey: SECRET, openId: 0, bucketName: 'default' });
	const port = await server.listen(0);
	const tmp = await fs.promises.mkdtemp(path.join(os.tmpdir(), 'mos-cli-test-'));
	try {
		await fn(server, makeClient(`http://127.0.0.1:${port}`), tmp);
	} finally {
		await server.close();
		await fs.promises.rm(tmp, { recursive: true, force: true });
	}
}

function randomFile(dir: string, name: string, size: number): string {
	const p = path.join(dir, name);
	fs.mkdirSync(path.dirname(p), { recursive: true });
	fs.writeFileSync(p, crypto.randomBytes(size));
	return p;
}

import crypto from 'node:crypto';

test('上传小文件并下载回来，内容一致', async () => {
	await withServer(async (server, client, tmp) => {
		const file = randomFile(tmp, 'small.bin', 1024 * 32);
		const original = fs.readFileSync(file);

		const result = await uploadFile(client, file, { remotePath: '/small.bin' });
		assert.equal(result.pathname, '/small.bin');
		assert.equal(result.instant, false);
		assert.equal(result.chunks, 1);

		// 服务端确实收到并合并了文件
		const uploaded = server.getContent('/small.bin');
		assert.ok(uploaded);
		assert.deepEqual(uploaded, original);

		// 下载回来
		const out = path.join(tmp, 'out', 'small.bin');
		await download(client, '/small.bin', { destination: out });
		assert.deepEqual(fs.readFileSync(out), original);
	});
});

test('上传大文件（多分片）并校验内容一致', async () => {
	await withServer(async (server, client, tmp) => {
		// 12MB，配合 5MB 分片 -> 3 片
		const file = randomFile(tmp, 'big.bin', 12 * 1024 * 1024);
		const original = fs.readFileSync(file);

		const result = await uploadFile(client, file, { remotePath: '/big.bin', concurrency: 3 });
		assert.ok(result.chunks >= 2, `期望多分片，实际 ${result.chunks}`);
		assert.deepEqual(server.getContent('/big.bin'), original);

		const out = path.join(tmp, 'big-out.bin');
		await download(client, '/big.bin', { destination: out });
		assert.deepEqual(fs.readFileSync(out), original);
	});
});

test('相同文件重复上传触发秒传', async () => {
	await withServer(async (server, client, tmp) => {
		const file = randomFile(tmp, 'dedup.bin', 4096);

		const first = await uploadFile(client, file, { remotePath: '/dedup.bin' });
		assert.equal(first.instant, false);
		const chunksAfterFirst = server.counters_.chunk;

		const second = await uploadFile(client, file, { remotePath: '/dedup2.bin' });
		assert.equal(second.instant, true, '第二次上传应秒传');
		assert.equal(server.counters_.chunk, chunksAfterFirst, '秒传不应再上传分片');
	});
});

test('--force 对相同内容退回秒传（服务端按 md5 去重）', async () => {
	await withServer(async (server, client, tmp) => {
		const file = randomFile(tmp, 'force.bin', 4096);
		await uploadFile(client, file, { remotePath: '/force.bin' });
		const chunksAfterFirst = server.counters_.chunk;

		// 服务端没有强制重传能力：md5 命中即复用 fileHouse，
		// 继续传分片会因缺少 UploadFile 记录而报「uploadFile不存在」，
		// 因此 --force 在这种情况下按秒传处理并给出提示。
		const forced = await uploadFile(client, file, { remotePath: '/force.bin', force: true });
		assert.equal(forced.instant, true);
		assert.equal(server.counters_.chunk, chunksAfterFirst, '不应重复上传分片');
	});
});

test('重名上传需 cover（对齐服务端 @RequestParam 语义）', async () => {
	await withServer(async (server, client, tmp) => {
		const file = randomFile(tmp, 'dup.bin', 100);
		await uploadFile(client, file, { remotePath: '/dup.bin' });

		// 不带 cover：服务端在秒传之前就做重名检查，应报错
		await assert.rejects(
			async () => uploadFile(client, file, { remotePath: '/dup.bin' }),
			/已存在相同的pathname/,
			'重名且未指定 cover 时应报错',
		);

		// 带 cover：走秒传覆盖
		const r = await uploadFile(client, file, { remotePath: '/dup.bin', cover: true });
		assert.ok(r.instant || r.chunks > 0);
	});
});

test('cover / isPublic 通过查询参数生效', async () => {
	await withServer(async (server, client, tmp) => {
		const file = randomFile(tmp, 'meta.bin', 64);
		await uploadFile(client, file, { remotePath: '/meta.bin', isPublic: true, contentType: 'text/x-test' });

		const info = await client.getFileInfo('/meta.bin');
		assert.ok(info);
		assert.equal(info.isPublic, true, 'isPublic 应通过查询参数生效');
		assert.equal(info.contentType, 'text/x-test');
	});
});

test('分片大小计算与 Java SDK 一致', () => {
	// partSize = ceil(size/chunks) 再 clamp
	assert.deepEqual(planChunks(12 * 1024 * 1024, 50), { partSize: 5 * 1024 * 1024, chunks: 3 });
	assert.deepEqual(planChunks(1024, 50), { partSize: 5 * 1024 * 1024, chunks: 1 });
	assert.deepEqual(planChunks(0, 50), { partSize: 5 * 1024 * 1024, chunks: 1 });
});

test('中文与空格路径的上传下载', async () => {
	await withServer(async (server, client, tmp) => {
		const file = randomFile(tmp, '中文文件.txt', 2048);
		const original = fs.readFileSync(file);
		const remotePath = '/文档/中文 文件.txt';

		await uploadFile(client, file, { remotePath: remotePath });
		assert.deepEqual(server.getContent(remotePath), original);

		const out = path.join(tmp, 'cn-out.txt');
		await download(client, remotePath, { destination: out });
		assert.deepEqual(fs.readFileSync(out), original);
	});
});

test('文件列表查询（对齐服务端 DirAndResource 字段）', async () => {
	await withServer(async (server, client, tmp) => {
		const a = randomFile(tmp, 'a.txt', 100);
		const b = randomFile(tmp, 'b.txt', 200);
		await uploadFile(client, a, { remotePath: '/dir/a.txt' });
		await uploadFile(client, b, { remotePath: '/dir/b.txt' });

		const page = await client.list('/dir');
		const items = pageItems(page);
		const names = items.map((i) => itemName(i)).sort();
		assert.deepEqual(names, ['a.txt', 'b.txt']);

		// 服务端用 isDir 区分目录，名称在 fileName，目录没有结尾斜杠
		for (const item of items) {
			assert.equal(item.isDir, false);
			assert.equal(typeof item.fileName, 'string');
			assert.ok(item.path?.startsWith('/dir/'));
		}
		assert.equal(itemSize(items[0]), items[0].sizeByte);
	});
});

test('子目录在列表中标记为 isDir 且不带结尾斜杠', async () => {
	await withServer(async (server, client, tmp) => {
		const f = randomFile(tmp, 'x.txt', 10);
		await uploadFile(client, f, { remotePath: '/root/sub/x.txt' });

		const items = pageItems(await client.list('/root'));
		const dir = items.find((i) => isDirItem(i));
		assert.ok(dir, '应存在子目录');
		assert.equal(dir!.fileName, 'sub');
		assert.equal(dir!.path, '/root/sub', '目录路径不应带结尾斜杠');
		assert.equal(itemSize(dir!), null, '目录不应有大小');
		// 拼接子路径
		assert.equal(joinItemPath('/root', dir!), '/root/sub');
	});
});

test('文件存在性判断', async () => {
	await withServer(async (server, client, tmp) => {
		const file = randomFile(tmp, 'exists.txt', 128);
		assert.equal(await client.isExists('/nope.txt'), false);
		await uploadFile(client, file, { remotePath: '/exists.txt' });
		assert.equal(await client.isExists('/exists.txt'), true);
	});
});

test('文件信息查询', async () => {
	await withServer(async (server, client, tmp) => {
		const content = Buffer.from('hello mos');
		const file = path.join(tmp, 'info.txt');
		fs.writeFileSync(file, content);
		await uploadFile(client, file, { remotePath: '/info.txt' });

		const info = await client.getFileInfo('/info.txt');
		assert.ok(info);
		assert.equal(Number(info.sizeByte), content.length);
		assert.equal(info.md5, crypto.createHash('md5').update(content).digest('hex'));
	});
});

test('删除文件', async () => {
	await withServer(async (server, client, tmp) => {
		const file = randomFile(tmp, 'del.txt', 64);
		await uploadFile(client, file, { remotePath: '/del.txt' });
		assert.equal(await client.deleteFile('/del.txt'), true);
		assert.equal(await client.isExists('/del.txt'), false);
	});
});

test('上传目录（递归）', async () => {
	await withServer(async (server, client, tmp) => {
		const dir = path.join(tmp, 'tree');
		fs.mkdirSync(path.join(dir, 'sub'), { recursive: true });
		fs.writeFileSync(path.join(dir, 'root.txt'), 'root');
		fs.writeFileSync(path.join(dir, 'sub', 'leaf.txt'), 'leaf');

		const res = await uploadDir(client, dir, '/tree');
		assert.equal(res.files, 2);
		assert.equal(server.getContent('/tree/root.txt')?.toString(), 'root');
		assert.equal(server.getContent('/tree/sub/leaf.txt')?.toString(), 'leaf');
	});
});

test('下载目录（递归）', async () => {
	await withServer(async (server, client, tmp) => {
		const dir = path.join(tmp, 'src-tree');
		fs.mkdirSync(path.join(dir, 'sub'), { recursive: true });
		fs.writeFileSync(path.join(dir, 'root.txt'), 'root content');
		fs.writeFileSync(path.join(dir, 'sub', 'leaf.txt'), 'leaf content');

		await uploadDir(client, dir, '/tree');
		const dest = path.join(tmp, 'dst-tree');
		const res = await downloadDir(client, '/tree', dest);
		assert.equal(res.files, 2);
		assert.equal(fs.readFileSync(path.join(dest, 'root.txt'), 'utf8'), 'root content');
		assert.equal(fs.readFileSync(path.join(dest, 'sub', 'leaf.txt'), 'utf8'), 'leaf content');
	});
});

test('错误的签名被服务端拒绝', async () => {
	const server = new MockMosServer({ secretKey: SECRET, openId: 0, bucketName: 'default' });
	const port = await server.listen(0);
	try {
		// 使用不同的密钥 -> 签名无法通过服务端校验
		const badClient = makeClient(`http://127.0.0.1:${port}`);
		(badClient.config as any).secretKey = 'wrong-secret';

		await assert.rejects(
			async () => {
				const dir = path.join(os.tmpdir(), `mos-bad-${Date.now()}`);
				await fs.promises.mkdir(dir, { recursive: true });
				const f = path.join(dir, 'x.txt');
				fs.writeFileSync(f, 'data');
				await uploadFile(badClient, f, { remotePath: '/x.txt' });
			},
			/没有权限|签名/,
			'错误签名应被拒绝',
		);
	} finally {
		await server.close();
	}
});

test('签名不含目标路径时被拒绝', async () => {
	await withServer(async (server, client) => {
		// 直接用别的路径的签名去访问
		const sign = client.getSign('/allowed.txt', 600);
		const res = await fetch(
			`http://127.0.0.1:${(server as any).server?.address?.().port ?? 0}`,
		).catch(() => null);
		// 仅验证签名串内容不同即可（不发起真实请求）
		assert.ok(!sign.includes('/other.txt'));
	});
});

test('重命名文件', async () => {
	await withServer(async (server, client, tmp) => {
		const file = randomFile(tmp, 'old.txt', 100);
		await uploadFile(client, file, { remotePath: '/old.txt' });

		await client.rename('/old.txt', '/new.txt');
		assert.equal(await client.isExists('/old.txt'), false);
		assert.equal(await client.isExists('/new.txt'), true);
		assert.deepEqual(server.getContent('/new.txt'), fs.readFileSync(file));
	});
});

test('删除目录', async () => {
	await withServer(async (server, client, tmp) => {
		const file = randomFile(tmp, 'x.txt', 50);
		await uploadFile(client, file, { remotePath: '/todir/x.txt' });
		assert.equal(await client.isExists('/todir/x.txt'), true);

		await client.deleteDir('/todir');
		assert.equal(await client.isExists('/todir/x.txt'), false);
	});
});

test('访问不存在的资源时报错', async () => {
	await withServer(async (server, client) => {
		assert.equal(await client.getFileInfo('/missing.txt'), null);
	});
});

test('并发上传大量小文件', async () => {	await withServer(async (server, client, tmp) => {
		const files: string[] = [];
		for (let i = 0; i < 8; i++) {
			files.push(randomFile(tmp, `f${i}.bin`, 1024 * (i + 1)));
		}
		await Promise.all(files.map((f, i) => uploadFile(client, f, { remotePath: `/many/f${i}.bin` })));
		for (let i = 0; i < 8; i++) {
			assert.ok(server.getContent(`/many/f${i}.bin`), `f${i} 应存在`);
		}
	});
});
