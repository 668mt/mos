/**
 * 服务端文件列表（DirAndResource）的字段处理。
 *
 * 对应服务端返回结构（mt.spring.mos.sdk.entity.DirAndResource）：
 *   isDir   Boolean  是否目录
 *   path    String   完整路径，如 /resources
 *   fileName String  名称，如 resources
 *   sizeByte Long     字节数，目录为 null
 *   lastModified Long 毫秒时间戳，可能为 0
 *
 * 注意：目录没有结尾斜杠，是靠 isDir 区分的。
 */

export interface DirAndResource {
	isDir?: boolean | number | null;
	id?: number;
	path?: string;
	sizeByte?: number | null;
	createdDate?: string | null;
	updatedDate?: string | null;
	contentType?: string | null;
	isPublic?: boolean;
	lastModified?: number | null;
	visits?: number;
	/** 服务端可能返回 fileName，也可能只给 path */
	fileName?: string | null;
	readableSize?: string | null;
	urlEncodePath?: string;
	image?: boolean;
}

export interface PageResult<T = DirAndResource> {
	list?: T[];
	data?: T[];
	total?: number;
	pageNum?: number;
	pageSize?: number;
	[key: string]: unknown;
}

/** 取列表数组，兼容 list / data 两种字段名 */
export function pageItems<T = DirAndResource>(page: PageResult<T> | null | undefined): T[] {
	if (!page) return [];
	if (Array.isArray(page.list)) return page.list;
	if (Array.isArray(page.data)) return page.data;
	return [];
}

/** 是否是目录 */
export function isDirItem(item: DirAndResource): boolean {
	return item.isDir === true || item.isDir === 1;
}

/**
 * 取条目名称。
 * 优先 fileName，其次从 path 的最后一段推导。
 */
export function itemName(item: DirAndResource): string {
	if (item.fileName) return item.fileName;
	const p = item.path ?? '';
	if (!p) return '';
	const parts = p.replace(/\/+$/, '').split('/');
	return parts[parts.length - 1] ?? '';
}

/**
 * 取条目的完整路径，目录会补上结尾斜杠便于拼接。
 */
export function itemPath(item: DirAndResource): string {
	const raw = item.path ?? '';
	if (!raw) return '';
	if (isDirItem(item) && !raw.endsWith('/')) {
		// 目录路径统一不带结尾斜杠，这里保持原样，由调用方按需拼接
		return raw;
	}
	return raw;
}

/** 拼接父目录与子条目路径 */
export function joinItemPath(parent: string, item: DirAndResource): string {
	const base = parent === '/' ? '' : parent.replace(/\/+$/, '');
	const name = itemName(item);
	return `${base}/${name}`;
}

/** 文件大小，目录返回 null */
export function itemSize(item: DirAndResource): number | null {
	if (isDirItem(item)) return null;
	const n = item.sizeByte;
	return typeof n === 'number' && Number.isFinite(n) ? n : null;
}

/** 修改时间，0 或缺失返回 null */
export function itemLastModified(item: DirAndResource): number | null {
	const n = item.lastModified;
	if (typeof n !== 'number' || !Number.isFinite(n) || n <= 0) return null;
	return n;
}
