/**
 * 真实启动 CLI 二进制，逐个跑一遍所有命令。
 *
 * 目的：捕获「回调签名 / 选项名 / 参数传递」这类只有真正执行才会暴露的问题。
 * 之前踩过的坑：
 *   - exists 命令参数遮蔽导致 cmd.opts is not a function
 *   - mv 有两个位置参数时 commander 不传 Command
 *   - --limit-speed 被解析成 limitSpeed，代码却读 flags.limit
 */

import test from 'node:test';
import assert from 'node:assert/strict';
import { spawn, spawnSync } from 'node:child_process';
import * as fs from 'node:fs';
import * as os from 'node:os';
import * as path from 'node:path';
import { fileURLToPath } from 'node:url';

const pkgRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const cliPath = path.join(pkgRoot, 'dist', 'index.js');
const mockEntry = path.join(pkgRoot, 'dist', 'test', 'mockServerMain.js');

const SECRET = 'e2e-secret';
const PORT = 18999;

let mockProc: ReturnType<typeof spawn> | null = null;
let workDir: string;
let configFile: string;

/**
 * 等待模拟服务端就绪。
 *
 * 模拟端必须跑在独立进程：测试里用 spawnSync 执行 CLI 会阻塞事件循环，
 * 若服务端与测试同进程，CLI 的请求就等不到响应（互相等待）。
 */
async function waitForServer(timeoutMs = 10_000): Promise<void> {
	const deadline = Date.now() + timeoutMs;
	while (Date.now() < deadline) {
		try {
			const res = await fetch(`http://127.0.0.1:${PORT}/index.html`, { signal: AbortSignal.timeout(500) });
			if (res.ok) return;
		} catch {
			// 还没起来，继续等
		}
		await new Promise((r) => setTimeout(r, 100));
	}
	throw new Error('模拟服务端启动超时');
}

function mos(args: string[], opts: { allowFail?: boolean } = {}): { code: number; out: string } {
	const res = spawnSync(process.execPath, [cliPath, ...args], {
		cwd: workDir,
		encoding: 'utf8',
		timeout: 30_000,
		env: {
			...process.env,
			MOS_CONFIG: configFile,
			// 关闭 Git Bash 的路径转换，避免 /foo 被改写成 C:/foo
			MSYS_NO_PATHCONV: '1',
			MSYS2_ARG_CONV_EXCL: '*',
		},
		// stdin 传 ignore：否则子进程等待输入会一直阻塞
		stdio: ['ignore', 'pipe', 'pipe'],
	});

	const out = `${res.stdout ?? ''}${res.stderr ?? ''}`;
	if (res.error) {
		if (!opts.allowFail) throw new Error(`mos ${args.join(' ')}: ${res.error.message}`);
		return { code: 1, out: `${out}\n${res.error.message}` };
	}
	const code = res.status ?? 1;
	if (code !== 0 && !opts.allowFail) {
		throw new Error(`mos ${args.join(' ')}\n退出码 ${code}\n${out}`);
	}
	return { code, out };
}

test.before(async () => {
	// 模拟端独立进程启动
	mockProc = spawn(process.execPath, [mockEntry, SECRET, 'e2e', String(PORT)], {
		stdio: ['ignore', 'pipe', 'pipe'],
	});
	mockProc.stdout?.resume();
	mockProc.stderr?.resume();
	await waitForServer();

	workDir = fs.mkdtempSync(path.join(os.tmpdir(), 'mos-e2e-'));
	configFile = path.join(workDir, 'config.json');

	// 写配置文件，避开交互式 config set
	fs.writeFileSync(
		configFile,
		JSON.stringify(
			{
				defaultProfile: 'default',
				profiles: {
					default: { host: `http://127.0.0.1:${PORT}`, bucket: 'e2e', secretKey: SECRET, openId: 0 },
					alt: { host: `http://127.0.0.1:${PORT}`, bucket: 'alt', secretKey: SECRET, openId: 0 },
				},
			},
			null,
			2,
		),
		'utf8',
	);

	fs.writeFileSync(path.join(workDir, 'hello.txt'), 'hello e2e', 'utf8');
	fs.mkdirSync(path.join(workDir, 'tree', 'sub'), { recursive: true });
	fs.writeFileSync(path.join(workDir, 'tree', 'a.txt'), 'aaa', 'utf8');
	fs.writeFileSync(path.join(workDir, 'tree', 'sub', 'b.txt'), 'bbb', 'utf8');
});

test.after(async () => {
	mockProc?.kill();
	if (workDir) fs.rmSync(workDir, { recursive: true, force: true });
});

test('mos --help 可用', () => {
	const { out } = mos(['--help']);
	assert.match(out, /upload/);
	assert.match(out, /download/);
});

test('config show / profiles / use', () => {
	assert.match(mos(['config', 'show']).out, /bucket\s*:\s*e2e/);
	assert.match(mos(['config', 'profiles']).out, /default/);
	assert.match(mos(['config', 'show', '--profile', 'alt']).out, /bucket\s*:\s*alt/);
});

test('upload 单文件并可 info / exists', () => {
	mos(['upload', 'hello.txt']);
	assert.match(mos(['info', 'hello.txt']).out, /hello\.txt/);

	const ex = mos(['exists', 'hello.txt']);
	assert.equal(ex.code, 0);
	assert.match(ex.out, /存在/);

	const missing = mos(['exists', 'nope.txt'], { allowFail: true });
	assert.equal(missing.code, 1);
	assert.match(missing.out, /不存在/);
});

test('upload 目录并递归下载回本地', () => {
	mos(['upload', 'tree', '-r', '/tree']);
	const out = mos(['download', '/tree', '-r', '-o', './back']).out;
	assert.match(out, /目录下载完成/);
	assert.ok(fs.existsSync(path.join(workDir, 'back', 'a.txt')));
	assert.ok(fs.existsSync(path.join(workDir, 'back', 'sub', 'b.txt')));
	assert.equal(fs.readFileSync(path.join(workDir, 'back', 'sub', 'b.txt'), 'utf8'), 'bbb');
});

test('ls 列出目录内容', () => {
	assert.match(mos(['ls', '/tree']).out, /a\.txt/);
	assert.match(mos(['ls', '/tree/sub']).out, /b\.txt/);
});

test('mv 重命名可用（两个位置参数）', () => {
	mos(['upload', 'hello.txt', '-r', '/to-rename.txt']);
	mos(['mv', '/to-rename.txt', '/renamed.txt']);
	assert.match(mos(['ls', '/']).out, /renamed\.txt/);
	assert.doesNotMatch(mos(['ls', '/']).out, /to-rename\.txt/);
});

test('rm 删除文件（带 --yes）', () => {
	mos(['upload', 'hello.txt', '-r', '/to-delete.txt']);
	assert.match(mos(['rm', '/to-delete.txt', '--yes']).out, /已删除/);
	// 删除后 exists 应返回非 0（不存在）
	const gone = mos(['exists', '/to-delete.txt'], { allowFail: true });
	assert.equal(gone.code, 1);
	assert.match(gone.out, /不存在/);
});

test('url 生成可解析的链接', () => {
	const url = mos(['url', 'hello.txt']).out.trim();
	assert.match(url, /^http:\/\/127\.0\.0\.1:\d+\/mos\/e2e\/hello\.txt\?sign=/);
	assert.doesNotMatch(url, /\s/);
});

test('url --short 生成短链接（单地址，不含逗号）', () => {
	const res = mos(['url', 'hello.txt', '--short', '--expire', '3600']);
	const url = res.out.trim();
	assert.match(url, /^http:\/\/127\.0\.0\.1:\d+\/s\/[A-Za-z0-9]+$/);
	// 回归：多地址配置下不能把逗号分隔串直接输出
	assert.doesNotMatch(url, /,/, '短链地址不应包含逗号分隔的多地址');
});

test('url --short --domain 使用指定域名', () => {
	const url = mos(['url', 'hello.txt', '--short', '--expire', '3600', '--domain', 'https://cdn.example.com']).out.trim();
	assert.match(url, /^https:\/\/cdn\.example\.com\/s\/[A-Za-z0-9]+$/);
});

test('url --short 校验有效期下限（>=60）', () => {
	const tooSmall = mos(['url', 'hello.txt', '--short', '--expire', '10'], { allowFail: true });
	assert.equal(tooSmall.code, 1);
	assert.match(tooSmall.out, /不能小于 60 秒/);

	const permanent = mos(['url', 'hello.txt', '--short', '--expire', '-1'], { allowFail: true });
	assert.equal(permanent.code, 1);
	assert.match(permanent.out, /不支持永久有效期/);

	// 60 秒为边界，应成功
	assert.match(mos(['url', 'hello.txt', '--short', '--expire', '60']).out, /\/s\//);
});

test('--limit-speed 选项被正确接收（不抛 TypeError）', () => {
	const res = mos(['download', 'hello.txt', '-o', './limited.txt', '--limit-speed', '1024', '--cover']);
	assert.match(res.out, /下载完成/);
	assert.ok(fs.existsSync(path.join(workDir, 'limited.txt')));
});

test('--profile 可用于数据命令', () => {
	// default 与 alt 使用不同 bucket，验证 --profile 确实切换了桶
	mos(['upload', 'hello.txt', '-r', '/profile-test.txt']);

	// alt 桶是「另一个桶」，服务端会因 bucket 校验失败而拒绝，
	// 这本身就能证明 --profile 被正确解析并应用了。
	const alt = mos(['ls', '/', '--profile', 'alt'], { allowFail: true });
	assert.notEqual(alt.code, 0, 'alt 桶不存在时应报错，说明 --profile 已生效');
	assert.match(alt.out, /bucket校验错误|alt/);

	// 默认档案应能看到刚上传的文件
	assert.match(mos(['ls', '/']).out, /profile-test\.txt/);
});

test('install 命令可用', () => {
	const target = path.join(workDir, 'skills-dir');
	const res = mos(['install', '--path', target]);
	assert.match(res.out, /安装完成|已更新/);
	assert.ok(fs.existsSync(path.join(target, 'mos-cli', 'SKILL.md')));
});

test('未知命令返回非 0 且给出提示', () => {
	const res = mos(['no-such-command'], { allowFail: true });
	assert.notEqual(res.code, 0);
});