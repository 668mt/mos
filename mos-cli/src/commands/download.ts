/**
 * mos download —— 下载文件 / 目录 / 输出到 stdout
 */

import { Command } from 'commander';
import { addGlobalOptions, createClient } from './context.js';
import { download, downloadDir } from '../core/download.js';
import { error } from '../util/ui.js';

interface DownloadFlags {
	out?: string;
	cover?: boolean;
	limit?: number;
	stdout?: boolean;
	thumb?: boolean;
	recursive?: boolean;
}

export function registerDownloadCommand(program: Command): Command {
	const cmd = addGlobalOptions(program.command('download'))
		.description('从 mos 下载文件或目录')
		.argument('<pathname>', '远端文件路径（目录需配合 --recursive）')
		.option('-o, --out <path>', '本地保存路径（文件或目录）')
		.option('-c, --cover', '覆盖已存在的本地文件', false)
		.option('--limit-speed <kb>', '限速，单位 KB/s', (v) => parseInt(v, 10))
		.option('--stdout', '输出到标准输出', false)
		.option('--thumb', '下载缩略图', false)
		.option('-r, --recursive', '递归下载整个目录', false)
		.action(async (pathname: string, flags: DownloadFlags, self: Command) => {
			try {
				const client = createClient(self);

				if (flags.recursive) {
					const local = flags.out ?? '.';
					await downloadDir(client, pathname, local, {
						cover: flags.cover,
						limitSpeedKbSeconds: flags.limit,
					});
					return;
				}

				await download(client, pathname, {
					destination: flags.out,
					cover: flags.cover,
					limitSpeedKbSeconds: flags.limit,
					toStdout: flags.stdout,
					thumb: flags.thumb,
				});
			} catch (e) {
				error((e as Error).message);
				process.exitCode = 1;
			}
		});

	return cmd;
}
