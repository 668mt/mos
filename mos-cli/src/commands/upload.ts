/**
 * mos upload —— 上传文件 / 目录 / stdin
 */

import * as fs from 'node:fs';
import { Command } from 'commander';
import { addGlobalOptions, createClient } from './context.js';
import { uploadDir, uploadFile, uploadStdin } from '../core/upload.js';
import { guessContentType } from '../core/upload.js';
import { error, warn } from '../util/ui.js';

interface UploadFlags {
	remote?: string;
	chunks?: number;
	minPartSize?: number;
	maxPartSize?: number;
	concurrency?: number;
	cover?: boolean;
	public?: boolean;
	contentType?: string;
	force?: boolean;
	stdin?: boolean;
	quiet?: boolean;
}

export function registerUploadCommand(program: Command): Command {
	const cmd = addGlobalOptions(program.command('upload'))
		.description('上传文件或目录到 mos')
		.argument('[path]', '本地文件或目录路径', '.')
		.option('-r, --remote <pathname>', '远端存储路径（文件或目录）')
		.option('-c, --chunks <n>', '期望分片数', (v) => parseInt(v, 10), 50)
		.option('--min-part-size <bytes>', '最小分片大小', (v) => parseInt(v, 10))
		.option('--max-part-size <bytes>', '最大分片大小', (v) => parseInt(v, 10))
		.option('-p, --concurrency <n>', '并发上传分片数', (v) => parseInt(v, 10), 5)
		.option('--cover', '覆盖同名文件', false)
		.option('--public', '文件设为公开可访问', false)
		.option('--content-type <type>', '指定 contentType')
		.option('--force', '强制上传，跳过秒传', false)
		.option('--stdin', '从标准输入读取内容上传', false)
		.option('-q, --quiet', '关闭进度输出', false)
		.action(async (targetPath: string, flags: UploadFlags, self: Command) => {
			try {
				const client = createClient(self);

				const options = {
					expectChunks: flags.chunks,
					minPartSize: flags.minPartSize,
					maxPartSize: flags.maxPartSize,
					concurrency: flags.concurrency,
					cover: flags.cover,
					isPublic: flags.public,
					remotePath: flags.remote,
					contentType: flags.contentType,
					force: flags.force,
				};

				// stdin 模式
				if (flags.stdin) {
					await uploadStdin(client, {
						...options,
						contentType: flags.contentType ?? 'application/octet-stream',
					});
					return;
				}

				const stat = await fs.promises.stat(targetPath);

				if (stat.isDirectory()) {
					if (flags.quiet) {
						// 目录上传逐个文件输出，quiet 时静默
					}
					await uploadDir(client, targetPath, flags.remote ?? '', options);
				} else {
					if (!flags.contentType) {
						const guessed = guessContentType(targetPath);
						if (!guessed) {
							warn(`无法识别 ${targetPath} 的类型，将使用 application/octet-stream`);
						}
					}
					await uploadFile(client, targetPath, options);
				}
			} catch (e) {
				error((e as Error).message);
				process.exitCode = 1;
			}
		});

	return cmd;
}
