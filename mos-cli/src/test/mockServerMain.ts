/**
 * e2e 测试用的模拟服务端启动脚本（独立进程运行）。
 *
 * 为什么要独立进程：测试里用 spawnSync 执行 CLI 会阻塞事件循环，
 * 若服务端与测试同进程，CLI 的 HTTP 请求就等不到响应（互相等待）。
 */
import { MockMosServer } from './mockServer.js';

const secretKey = process.argv[2] ?? 'e2e-secret';
const bucketName = process.argv[3] ?? 'e2e';
const port = Number(process.argv[4] ?? 18999);

const server = new MockMosServer({ secretKey, openId: 0, bucketName });
await server.listen(port);
console.log(`mock ready on ${port}`);

const shutdown = () => {
	void server.close().then(() => process.exit(0));
};
process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);