// 构建后把测试需要的静态资源（Java 生成的基准数据）复制到 dist
import { cpSync, existsSync, mkdirSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

// fileURLToPath 对目录 URL 会保留结尾的 /，这里显式取掉
const root = fileURLToPath(new URL('.', import.meta.url)).replace(/[\\/]+$/, '');
const toDir = join(root, 'dist', 'test');
mkdirSync(toDir, { recursive: true });

// 测试用静态资源（由 Java 生成的基准数据）
for (const name of ['ref.txt', 'enc-ref.txt']) {
	const from = join(root, 'src', 'test', name);
	if (!existsSync(from)) {
		console.error(`copy-assets: 找不到 ${from}`);
		process.exit(1);
	}
	cpSync(from, join(toDir, name));
}
