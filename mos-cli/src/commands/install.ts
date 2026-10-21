/**
 * mos install —— 把 mos-cli 的 skill 安装到本机 skill 目录（默认 ~/.agents/skills）
 *
 * skill 的约定是一个目录 + SKILL.md（YAML frontmatter: name / description），
 * 可选附带 scripts/ 与 references/ 子目录。
 */

import * as fs from 'node:fs';
import * as path from 'node:path';
import * as os from 'node:os';
import { fileURLToPath } from 'node:url';
import { Command } from 'commander';
import { error, info, ok } from '../util/ui.js';

/** skill 名称，需与 SKILL.md frontmatter 的 name 一致 */
const SKILL_NAME = 'mos-cli';

/** 默认安装根目录 */
export function defaultSkillsRoot(): string {
	return path.join(os.homedir(), '.agents', 'skills');
}

/**
 * 定位随包分发的 skill 源目录。
 *
 * 兼容两种运行方式：
 *   1. 源码直接运行 dist/commands/install.js -> <pkg>/skills
 *   2. 作为 npm 包安装 -> <pkg>/skills（已在 package.json 的 files 中声明）
 */
function resolveSkillSource(): string {
	// dist/commands/install.js -> 包根目录
	const here = path.dirname(fileURLToPath(import.meta.url));
	const pkgRoot = path.resolve(here, '..', '..');
	const candidate = path.join(pkgRoot, 'skills', SKILL_NAME);
	return candidate;
}

interface SkillManifest {
	name: string;
	description: string;
}

/**
 * 解析 SKILL.md 的 YAML frontmatter（只取顶层 name / description，
 * 支持单行与 `|` 块写法，不引入额外依赖）。
 */
export function parseSkillManifest(file: string): SkillManifest | null {
	if (!fs.existsSync(file)) return null;
	const text = fs.readFileSync(file, 'utf8');

	const match = /^---\r?\n([\s\S]*?)\r?\n---/.exec(text);
	if (!match) return null;
	const front = match[1];

	const fields: Record<string, string> = {};
	let currentKey: string | null = null;

	for (const line of front.split(/\r?\n/)) {
		const kv = /^([A-Za-z_][\w-]*)\s*:\s*(.*)$/.exec(line);
		if (kv) {
			currentKey = kv[1];
			const value = kv[2].trim();
			// `|` 与 `>` 为块标量，后续缩进行都是内容
			if (value === '|' || value === '>' || value === '|-' || value === '>-') {
				fields[currentKey] = '';
			} else {
				fields[currentKey] = value.replace(/^["']|["']$/g, '');
				currentKey = null;
			}
			continue;
		}
		// 块标量的续行
		if (currentKey && /^\s+\S/.test(line)) {
			fields[currentKey] = (fields[currentKey] ? fields[currentKey] + ' ' : '') + line.trim();
		}
	}

	if (!fields.name) return null;
	return { name: fields.name, description: fields.description ?? '' };
}

/** 递归复制目录 */
function copyDir(src: string, dest: string): number {
	let count = 0;
	fs.mkdirSync(dest, { recursive: true });
	for (const entry of fs.readdirSync(src, { withFileTypes: true })) {
		const s = path.join(src, entry.name);
		const d = path.join(dest, entry.name);
		if (entry.isDirectory()) {
			count += copyDir(s, d);
		} else if (entry.isFile()) {
			fs.copyFileSync(s, d);
			count++;
		}
	}
	return count;
}

interface InstallFlags {
	path?: string;
}

export function registerInstallCommand(program: Command): void {
	program
		.command('install')
		.description('把 mos-cli 的 skill 安装到 ~/.agents/skills/（已存在则覆盖）')
		.option('-p, --path <dir>', '安装到指定目录（默认 ~/.agents/skills）')
		.action((flags: InstallFlags) => {
			try {
				const source = resolveSkillSource();
				const skillFile = path.join(source, 'SKILL.md');

				if (!fs.existsSync(skillFile)) {
					error(`找不到内置 skill：${skillFile}\n提示：可能安装包不完整，请重新安装 mos-cli。`);
					process.exitCode = 1;
					return;
				}

				const manifest = parseSkillManifest(skillFile);
				if (!manifest) {
					error(`SKILL.md 的 frontmatter 无效（缺少 name）：${skillFile}`);
					process.exitCode = 1;
					return;
				}

				const root = flags.path
					? path.resolve(flags.path)
					: process.env.AGENTS_SKILLS_PATH
						? path.resolve(process.env.AGENTS_SKILLS_PATH)
						: defaultSkillsRoot();
				const target = path.join(root, SKILL_NAME);
				const existed = fs.existsSync(target);

				// 先安装到临时目录，成功后再切换，避免中途失败留下坏目录
				const staging = path.join(root, `.${SKILL_NAME}.installing`);
				fs.rmSync(staging, { recursive: true, force: true });
				fs.mkdirSync(staging, { recursive: true });

				let files: number;
				try {
					files = copyDir(source, staging);
					// 校验安装结果确实是合法 skill
					const installed = parseSkillManifest(path.join(staging, 'SKILL.md'));
					if (!installed) {
						throw new Error('安装后的 SKILL.md 校验失败');
					}
					fs.mkdirSync(root, { recursive: true });
					if (existed) {
						fs.rmSync(target, { recursive: true, force: true });
					}
					fs.renameSync(staging, target);
				} catch (e) {
					fs.rmSync(staging, { recursive: true, force: true });
					throw e;
				}

				ok(`skill ${existed ? '已更新' : '安装'}完成：${target}`);
				console.log(`  name        : ${manifest.name}`);
				console.log(`  files       : ${files}`);
				const desc = manifest.description.replace(/\s+/g, ' ').trim();
				if (desc) {
					console.log(`  description : ${desc.length > 80 ? desc.slice(0, 80) + '…' : desc}`);
				}
				info('重启 / 重新加载 agent 后即可使用该 skill。');
			} catch (e) {
				error((e as Error).message);
				process.exitCode = 1;
			}
		});
}
