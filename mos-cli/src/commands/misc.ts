/**
 * mos ls / info / rm / mv / url —— 其余开放接口能力
 */

import { Command } from 'commander';
import { addGlobalOptions, createClient } from './context.js';
import { isDirItem, itemLastModified, itemName, itemSize, joinItemPath, pageItems } from '../core/dirTypes.js';
import { error, ok, readableSize, readableTime } from '../util/ui.js';

// ------------------------------------------------------------------ ls
export function registerLsCommand(program: Command): Command {
	const cmd = addGlobalOptions(program.command('ls'))
		.alias('list')
		.description('列出目录下的文件和子目录')
		.argument('[pathname]', '远端目录路径', '/')
		.option('-k, --key-word <kw>', '按关键字过滤')
		.option('--page <n>', '页码，从 1 开始', (v) => parseInt(v, 10), 1)
		.option('--size <n>', '每页条数', (v) => parseInt(v, 10), 100)
		.option('--json', '以 JSON 输出', false)
		.action(async (pathname: string, opts: { keyWord?: string; page: number; size: number; json: boolean }, self: Command) => {
			try {
				const client = createClient(self);
				const page = await client.list(pathname, {
					keyWord: opts.keyWord,
					pageNum: opts.page,
					pageSize: opts.size,
				});

				if (opts.json) {
					console.log(JSON.stringify(page, null, 2));
					return;
				}

				const list = pageItems(page);
				if (list.length === 0) {
					console.log('(空目录)');
					return;
				}

				for (const item of list) {
					const dir = isDirItem(item);
					const name = itemName(item);
					const modified = itemLastModified(item);
					const modifiedStr = modified ? new Date(modified).toLocaleString() : '-';
					const size = itemSize(item);
					const type = dir ? 'DIR ' : 'FILE';
					const sizeStr = size === null ? '-' : readableSize(size);
					console.log(`${type}  ${sizeStr.padStart(10)}  ${modifiedStr.padEnd(20)}  ${name}`);
				}

				const total = page.total ?? list.length;
				console.log(`\n共 ${list.length} 条${total !== list.length ? `（总计 ${total}）` : ''}`);
			} catch (e) {
				error((e as Error).message);
				process.exitCode = 1;
			}
		});

	return cmd;
}

// ------------------------------------------------------------------ info
export function registerInfoCommand(program: Command): Command {
	const cmd = addGlobalOptions(program.command('info'))
		.description('查看文件信息')
		.argument('<pathname>', '远端文件路径')
		.option('--json', '以 JSON 输出', false)
		.action(async (pathname: string, opts: { json: boolean }, self: Command) => {
			try {
				const client = createClient(self);
				const info_ = await client.getFileInfo(pathname);
				if (!info_) {
					error(`资源不存在：${pathname}`);
					process.exitCode = 1;
					return;
				}
				if (opts.json) {
					console.log(JSON.stringify(info_, null, 2));
					return;
				}
				// 服务端的 info 不返回 pathname，用查询时的路径兜底
				const shownPath = info_.pathname ?? info_.urlEncodePath ?? pathname;
				const name = info_.name ?? info_.fileName;
				if (name) {
					console.log(`name        : ${name}`);
				}
				console.log(`pathname    : ${shownPath}`);
				console.log(`md5         : ${info_.md5 ?? '-'}`);
				const bytes = Number(info_.sizeByte ?? 0);
				console.log(`size        : ${info_.readableSize ?? readableSize(bytes)} (${bytes} bytes)`);
				console.log(`contentType : ${info_.contentType ?? '-'}`);
				console.log(`isPublic    : ${info_.isPublic ?? false}`);
				if (info_.visits !== undefined) {
					console.log(`visits      : ${info_.visits}`);
				}
				const lm = Number(info_.lastModified ?? 0);
				console.log(`lastModified: ${lm > 0 ? new Date(lm).toLocaleString() : '-'}`);
				if (info_.createdDate) {
					console.log(`createdDate : ${info_.createdDate}`);
				}
			} catch (e) {
				error((e as Error).message);
				process.exitCode = 1;
			}
		});

	return cmd;
}

// ------------------------------------------------------------------ rm
export function registerRmCommand(program: Command): Command {
	const cmd = addGlobalOptions(program.command('rm'))
		.alias('delete')
		.description('删除文件或目录')
		.argument('<pathname>', '远端路径（以 / 结尾视为目录）')
		.option('-r, --recursive', '递归删除目录', false)
		.option('--yes', '跳过确认', false)
		.action(async (pathname: string, opts: { recursive: boolean; yes: boolean }, self: Command) => {
			try {
				const client = createClient(self);

				const isDir = opts.recursive || pathname.endsWith('/');
				if (!opts.yes) {
					const answer = await confirm(`确定要删除 ${pathname} 吗？`);
					if (!answer) {
						console.log('已取消');
						return;
					}
				}

				if (isDir) {
					// 递归删除目录下的所有文件，再删除目录本身
					if (opts.recursive) {
						await deleteRecursive(client, pathname);
					}
					const done = await client.deleteDir(pathname);
					ok(done ? `已删除目录：${pathname}` : `目录删除未成功：${pathname}`);
				} else {
					const done = await client.deleteFile(pathname);
					ok(done ? `已删除文件：${pathname}` : `文件删除未成功：${pathname}`);
				}
			} catch (e) {
				error((e as Error).message);
				process.exitCode = 1;
			}
		});

	return cmd;
}

async function deleteRecursive(client: ReturnType<typeof createClient>, dir: string): Promise<void> {
	const page = await client.list(dir, { pageSize: 1000 });
	for (const item of pageItems(page)) {
		const child = joinItemPath(dir, item);
		if (isDirItem(item)) {
			await deleteRecursive(client, child);
			await client.deleteDir(child);
		} else {
			await client.deleteFile(child);
		}
	}
}

// ------------------------------------------------------------------ mv
export function registerMvCommand(program: Command): Command {
	const cmd = addGlobalOptions(program.command('mv'))
		.alias('rename')
		.description('重命名 / 移动文件')
		.argument('<pathname>', '原路径')
		.argument('<desPathname>', '新路径')
		// 两个位置参数时 commander 不会追加 Command，所以直接用 cmd 本身
		.action(async (pathname: string, desPathname: string) => {
			try {
				const client = createClient(cmd);
				await client.rename(pathname, desPathname);
				ok(`已重命名：${pathname} -> ${desPathname}`);
			} catch (e) {
				error((e as Error).message);
				process.exitCode = 1;
			}
		});

	return cmd;
}

/** 构造传给短链的查询串（与 Java SDK buildQueryString 行为一致） */
function buildRenderQuery(opts: { render?: boolean; thumb?: boolean }): string | undefined {
	const parts: string[] = [];
	if (opts.render) parts.push('render=true');
	if (opts.thumb) parts.push('thumb=true');
	return parts.length > 0 ? parts.join('&') : undefined;
}

// ------------------------------------------------------------------ url
export function registerUrlCommand(program: Command): Command {
	const cmd = addGlobalOptions(program.command('url'))
		.description('生成文件访问链接')
		.argument('<pathname>', '远端文件路径')
		.option('--expire <seconds>', '签名有效期（秒），-1 永久', (v) => parseInt(v, 10), 7200)
		.option('--thumb', '生成缩略图链接', false)
		.option('--render', '生成渲染链接', false)
		.option('--short', '生成短链接', false)
		.option('--domain <url>', '短链接使用的对外域名（默认用实际选中的服务地址）')
		.action(
			async (
				pathname: string,
				opts: { expire: number; thumb: boolean; render: boolean; short: boolean; domain?: string },
				self: Command,
			) => {
				try {
					const client = createClient(self);
					if (opts.short) {
						// 服务端要求 expireSeconds 为正且 >= 60，提前校验给出更清晰的提示
						if (opts.expire <= 0) {
							error('短链接不支持永久有效期，请用 --expire 指定大于 0 的秒数');
							process.exitCode = 1;
							return;
						}
						if (opts.expire < 60) {
							error(`短链接有效期不能小于 60 秒，当前为 ${opts.expire}`);
							process.exitCode = 1;
							return;
						}
						const code = await client.createShortUrl(pathname, {
							expireSeconds: opts.expire,
							queryString: buildRenderQuery(opts),
						});
						// 短链面向外部用户，应使用对外域名；
						// 未指定 --domain 时回退到实际选中的服务地址（而非整串多地址）
						const base = (opts.domain ?? client.host).replace(/\/+$/, '');
						console.log(`${base}/s/${code}`);
						return;
					}
					console.log(
						client.getResourceUrl(pathname, {
							expireSeconds: opts.expire,
							thumb: opts.thumb,
							render: opts.render,
						}),
					);
				} catch (e) {
					error((e as Error).message);
					process.exitCode = 1;
				}
			},
		);

	return cmd;
}

// ------------------------------------------------------------------ exists
export function registerExistsCommand(program: Command): Command {
	const existsCmd = addGlobalOptions(program.command('exists'))
		.description('判断文件是否存在')
		.argument('<pathname>', '远端文件路径')
		.action(async (pathname: string, _opts: unknown, self: Command) => {
			try {
				const client = createClient(self);
				const exists = await client.isExists(pathname);
				console.log(exists ? '存在' : '不存在');
				process.exitCode = exists ? 0 : 1;
			} catch (e) {
				error((e as Error).message);
				process.exitCode = 1;
			}
		});

	return existsCmd;
}

async function confirm(question: string): Promise<boolean> {
	if (!process.stdin.isTTY) {
			// 非交互环境默认不删除，避免误操作
			return false;
	}
	const readline = await import('node:readline');
	const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
	const answer = await new Promise<string>((resolve) => {
			rl.question(`${question} (y/N) `, resolve);
	});
	rl.close();
	return /^y(es)?$/i.test(answer.trim());
}
