/** 终端颜色与输出工具 */

const isColorSupported = (): boolean => {
	if (process.env.NO_COLOR) return false;
	if (process.env.FORCE_COLOR) return true;
	if (process.env.TERM === 'dumb') return false;
	return process.stdout.isTTY === true;
};

const enabled = isColorSupported();

function wrap(open: number, close: number) {
	return (s: string): string => (enabled ? `\u001B[${open}m${s}\u001B[${close}m` : s);
}

export const bold = wrap(1, 22);
export const dim = wrap(2, 22);
export const red = wrap(31, 39);
export const green = wrap(32, 39);
export const yellow = wrap(33, 39);
export const blue = wrap(34, 39);
export const cyan = wrap(36, 39);

export function ok(msg: string): void {
	console.log(`${green('✔')} ${msg}`);
}

export function info(msg: string): void {
	console.log(`${blue('ℹ')} ${msg}`);
}

export function warn(msg: string): void {
	console.log(`${yellow('!')} ${msg}`);
}

export function fail(msg: string): void {
	console.error(`${red('✖')} ${msg}`);
}

/** 供 commander 使用的错误输出 */
export function error(msg: string): void {
	console.error(`${red('错误:')} ${msg}`);
}

/** 可读的字节大小 */
export function readableSize(bytes: number): string {
	if (bytes < 1024) return `${bytes} B`;
	const units = ['KB', 'MB', 'GB', 'TB', 'PB'];
	let value = bytes / 1024;
	let idx = 0;
	while (value >= 1024 && idx < units.length - 1) {
		value /= 1024;
		idx++;
	}
	return `${value.toFixed(2)} ${units[idx]}`;
}

/** 可读的耗时 */
export function readableTime(ms: number): string {
	if (ms < 1000) return `${ms}ms`;
	const seconds = ms / 1000;
	if (seconds < 60) return `${seconds.toFixed(2)}s`;
	const minutes = Math.floor(seconds / 60);
	const rest = (seconds % 60).toFixed(0);
	return `${minutes}m${rest}s`;
}

export function readableSpeed(bytesPerSecond: number): string {
	return `${readableSize(bytesPerSecond)}/s`;
}
