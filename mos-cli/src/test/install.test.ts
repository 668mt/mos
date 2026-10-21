/**
 * mos install 相关测试：SKILL.md frontmatter 解析 + 安装流程。
 */

import test from 'node:test';
import assert from 'node:assert/strict';
import * as fs from 'node:fs';
import * as os from 'node:os';
import * as path from 'node:path';
import { fileURLToPath } from 'node:url';
import { defaultSkillsRoot, parseSkillManifest } from '../commands/install.js';

function tmpDir(): string {
	return fs.mkdtempSync(path.join(os.tmpdir(), 'mos-install-'));
}

/** dist/test -> 包根目录 */
const pkgRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const bundledSkill = path.join(pkgRoot, 'skills', 'mos-cli');

test('内置 skill 存在且格式合法', () => {
	const file = path.join(bundledSkill, 'SKILL.md');
	assert.ok(fs.existsSync(file), `找不到内置 skill：${file}`);

	const manifest = parseSkillManifest(file);
	assert.ok(manifest, 'frontmatter 解析失败');
	assert.equal(manifest.name, 'mos-cli');
	assert.ok(manifest.description.length > 0, 'description 不能为空');
	assert.ok(manifest.description.includes('mos'), 'description 应描述 mos 用途');
});

test('解析单行 frontmatter', () => {
	const dir = tmpDir();
	const file = path.join(dir, 'SKILL.md');
	fs.writeFileSync(
		file,
		['---', 'name: demo', 'description: 一个示例 skill', '---', '', '# body', ''].join('\n'),
		'utf8',
	);
	const m = parseSkillManifest(file);
	assert.equal(m?.name, 'demo');
	assert.equal(m?.description, '一个示例 skill');
	fs.rmSync(dir, { recursive: true, force: true });
});

test('解析块标量 frontmatter（| 写法）', () => {
	const dir = tmpDir();
	const file = path.join(dir, 'SKILL.md');
	fs.writeFileSync(
		file,
		[
			'---',
			'name: block-demo',
			'description: |',
			'  第一行内容，',
			'  第二行内容。',
			'---',
			'',
			'# body',
			'',
		].join('\n'),
		'utf8',
	);
	const m = parseSkillManifest(file);
	assert.equal(m?.name, 'block-demo');
	// 多行应被合并成一行
	assert.ok(!m!.description.includes('\n'), '块标量应合并为单行');
	assert.ok(m!.description.includes('第一行内容'));
	assert.ok(m!.description.includes('第二行内容'));
	fs.rmSync(dir, { recursive: true, force: true });
});

test('缺少 frontmatter 或 name 时返回 null', () => {
	const dir = tmpDir();
	const noFm = path.join(dir, 'a.md');
	fs.writeFileSync(noFm, '# 没有 frontmatter\n', 'utf8');
	assert.equal(parseSkillManifest(noFm), null);

	const noName = path.join(dir, 'b.md');
	fs.writeFileSync(noName, '---\ndescription: 只有描述\n---\n', 'utf8');
	assert.equal(parseSkillManifest(noName), null);

	assert.equal(parseSkillManifest(path.join(dir, 'missing.md')), null);
	fs.rmSync(dir, { recursive: true, force: true });
});

test('支持 CRLF 换行', () => {
	const dir = tmpDir();
	const file = path.join(dir, 'SKILL.md');
	fs.writeFileSync(file, '---\r\nname: crlf-demo\r\ndescription: windows 换行\r\n---\r\nbody\r\n', 'utf8');
	const m = parseSkillManifest(file);
	assert.equal(m?.name, 'crlf-demo');
	assert.equal(m?.description, 'windows 换行');
	fs.rmSync(dir, { recursive: true, force: true });
});

test('默认安装根目录是 ~/.agents/skills', () => {
	const root = defaultSkillsRoot();
	assert.ok(root.endsWith(path.join('.agents', 'skills')), `实际为 ${root}`);
});

test('安装到自定义目录后文件结构正确', () => {
	// 模拟 install 的核心流程（不经过 commander）
	const root = tmpDir();
	const target = path.join(root, 'mos-cli');

	fs.mkdirSync(target, { recursive: true });
	fs.cpSync(bundledSkill, target, { recursive: true });

	const installed = path.join(target, 'SKILL.md');
	assert.ok(fs.existsSync(installed), 'SKILL.md 应被安装');
	const m = parseSkillManifest(installed);
	assert.equal(m?.name, 'mos-cli');

	fs.rmSync(root, { recursive: true, force: true });
});

test('重复安装会覆盖并清理旧版本残留文件', () => {
	const root = tmpDir();
	const target = path.join(root, 'mos-cli');

	// 第一次安装
	fs.mkdirSync(target, { recursive: true });
	fs.cpSync(bundledSkill, target, { recursive: true });

	// 模拟旧版本残留的文件
	fs.writeFileSync(path.join(target, 'STALE.txt'), 'old', 'utf8');

	// 第二次安装：应覆盖，且清掉残留
	fs.rmSync(target, { recursive: true, force: true });
	fs.cpSync(bundledSkill, target, { recursive: true });

	assert.ok(fs.existsSync(path.join(target, 'SKILL.md')), 'SKILL.md 应存在');
	assert.ok(!fs.existsSync(path.join(target, 'STALE.txt')), '旧版本残留文件应被清理');

	fs.rmSync(root, { recursive: true, force: true });
});
