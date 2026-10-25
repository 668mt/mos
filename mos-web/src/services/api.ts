import { del, get, getRaw, post, put, transformFormData } from './request';
import type {
  AccessControl,
  Bucket,
  BucketGrantDto,
  BucketGrantResult,
  BucketPerm,
  BucketPermVo,
  BucketVo,
  ChartFlowData,
  ChartRequestData,
  CheckFileExistsResult,
  Client,
  Dir,
  DirAndResourceVo,
  DirDetailInfo,
  FileSuffixMap,
  PageInfo,
  ResourceListResult,
  StatisticInfo,
  UploadInitResult,
  User,
} from './types';

/* ------------------------------------------------------------------ 认证 */

/**
 * 登录。
 *
 * 服务端 `SecurityConfig.loginProcessingUrl("/login")`，使用表单登录，
 * 成功后返回 HTTP 200 + { status: 'ok', message: '登录成功!', result: User }。
 */
export async function login(params: {
  username: string;
  password: string;
  rememberMe?: boolean;
  code?: string;
}) {
  const res = await post<User>('/login', transformFormData(params), {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  });
  return res;
}

/** 查询是否需要输入验证码（失败 3 次后需要） */
export async function needCaptcha(username: string): Promise<boolean> {
  return get<boolean>(`/kaptcha/check/${encodeURIComponent(username)}`);
}

/** 验证码图片地址（带时间戳避免缓存） */
export function captchaUrl(): string {
  return `/kaptcha?t=${Date.now()}`;
}

/* ------------------------------------------------------------------ 桶 */

/** 桶列表（含自己创建的与被授权的） */
export function fetchBuckets(): Promise<BucketVo[]> {
  return get<BucketVo[]>('/member/bucket/list');
}

/** 新增桶 */
export function addBucket(data: {
  bucketName: string;
  defaultIsPublic: boolean;
  dataFragmentsAmount: number;
}) {
  return post<null>('/member/bucket', data);
}

/** 更新桶 */
export function updateBucket(data: {
  id: number;
  bucketName: string;
  defaultIsPublic: boolean;
  dataFragmentsAmount: number;
}) {
  return put<null>('/member/bucket', data);
}

/** 删除桶 */
export function deleteBucket(id: number) {
  return del<number>('/member/bucket', { params: { id } });
}

/* ------------------------------------------------------------- 桶授权 */

/** 桶的授权详情（仅桶所有者可查） */
export function fetchBucketGrants(bucketId: number): Promise<BucketGrantResult> {
  return get<BucketGrantResult>('/member/bucket/grant/list', { params: { bucketId } });
}

/** 全部可选权限 */
export function fetchAllPerms(): Promise<BucketPerm[]> {
  return get<BucketPerm[]>('/member/bucket/grant/perms/all');
}

/** 当前用户在各桶上的权限 */
export function fetchOwnPerms(): Promise<BucketPermVo[]> {
  return get<BucketPermVo[]>('/member/bucket/grant/perms/own');
}

/** 设置桶授权（全量覆盖） */
export function saveBucketGrants(data: BucketGrantDto) {
  return post<null>('/member/bucket/grant', data);
}

/* -------------------------------------------------------------- 秘钥 */

/** 桶下的秘钥列表 */
export function fetchAccessControls(bucketName: string): Promise<AccessControl[]> {
  return get<AccessControl[]>(`/member/access/${bucketName}`);
}

/** 新增秘钥 */
export function addAccessControl(bucketName: string, data: { useInfo: string }) {
  return post<AccessControl>(`/member/access/${bucketName}`, data);
}

/** 更新秘钥 */
export function updateAccessControl(
  bucketName: string,
  openId: number,
  data: { useInfo: string },
) {
  return put<number>(`/member/access/${bucketName}/${openId}`, data);
}

/** 删除秘钥 */
export function deleteAccessControl(bucketName: string, openId: number) {
  return del<number>(`/member/access/${bucketName}/${openId}`);
}

/** 生成签名访问链（短链） */
export function signAccess(data: {
  openId: number;
  bucketName: string;
  resourceId?: number | null;
  dirId?: number | null;
  expireSeconds: number;
  render?: boolean;
}) {
  return post<string>('/member/access/sign', data);
}

/* -------------------------------------------------------------- 资源 */

/** 资源列表请求参数 */
export interface ResourceListParams {
  pageNum?: number;
  pageSize?: number;
  path?: string;
  keyWord?: string;
  sortField?: string | null;
  sortOrder?: string | null;
  isDelete?: boolean;
  resourceId?: number;
  suffixs?: string[];
  isFile?: boolean;
  isDir?: boolean;
}

/** 资源列表 */
export function fetchResources(
  bucketName: string,
  params: ResourceListParams,
): Promise<ResourceListResult> {
  return get<ResourceListResult>(`/member/resource/${bucketName}/list`, { params });
}

/** 软删除（移入回收站） */
export function deleteResources(
  bucketName: string,
  params: { fileIds?: string | number; dirIds?: string | number },
) {
  return del<null>(`/member/resource/${bucketName}/del`, { params });
}

/** 彻底删除 */
export function realDeleteResources(
  bucketName: string,
  params: { fileIds?: string | number; dirIds?: string | number },
) {
  return del<null>(`/member/resource/${bucketName}/realDel`, { params });
}

/** 还原（从回收站恢复） */
export function recoverResources(
  bucketName: string,
  data: { fileIds: number[]; dirIds: number[] },
) {
  return put<null>(`/member/resource/${bucketName}/recover`, data);
}

/** 更新资源（重命名 / 权限 / contentType） */
export function updateResource(
  bucketName: string,
  id: number,
  data: { pathname: string; isPublic: boolean; contentType?: string | null },
) {
  return put<null>(`/member/resource/${bucketName}/${id}`, data);
}

/** 批量重名检查 */
export function checkFileExists(
  bucketName: string,
  pathnames: string[],
): Promise<CheckFileExistsResult> {
  return post<CheckFileExistsResult>(
    `/member/resource/${bucketName}/checkFile/isExists`,
    { pathnames },
  );
}

/** 复制资源到目标桶 */
export function copyResources(
  srcBucketName: string,
  desBucketName: string,
  data: { desPath: string; dirIds: number[]; resourceIds: number[] },
) {
  return post<null>(
    `/member/resource/copy/${srcBucketName}/to/${desBucketName}`,
    data,
  );
}

/** 移动资源到目标桶 */
export function moveResources(
  srcBucketName: string,
  desBucketName: string,
  data: { desPath: string; dirIds: number[]; resourceIds: number[] },
) {
  return post<null>(
    `/member/resource/move/${srcBucketName}/to/${desBucketName}`,
    data,
  );
}

/** 文件后缀分类（裸 JSON，非 ResResult 封装） */
export function fetchFileSuffix(): Promise<FileSuffixMap> {
  return getRaw<FileSuffixMap>('/member/resource/file/suffix');
}

/** 按类型获取文件列表（裸 PageInfo） */
export function fetchFilesByType(
  bucketName: string,
  type: string,
  params: { path: string; pageNum?: number; pageSize?: number },
): Promise<PageInfo<DirAndResourceVo>> {
  return getRaw<PageInfo<DirAndResourceVo>>(
    `/member/resource/${bucketName}/files/${type}`,
    { params },
  );
}

/** 按类型获取单个文件（裸对象） */
export function fetchFileByType(
  bucketName: string,
  type: string,
  resourceId: number,
  path: string,
): Promise<DirAndResourceVo> {
  return getRaw<DirAndResourceVo>(
    `/member/resource/${bucketName}/file/${type}/${resourceId}`,
    { params: { path } },
  );
}

/* -------------------------------------------------------------- 目录 */

/** 目录详情（含缩略图预览，裸 JSON） */
export function fetchDirDetailInfo(
  bucketName: string,
  dirId: number,
): Promise<DirDetailInfo> {
  return getRaw<DirDetailInfo>(`/member/dir/${bucketName}/detailInfo/${dirId}`);
}

/** 按路径模糊查找目录 */
export function selectDirs(
  bucketName: string,
  path: string,
  pageSize = 30,
): Promise<Dir[]> {
  return get<Dir[]>(`/member/dir/${bucketName}/select`, {
    params: { path, pageSize },
  });
}

/** 按精确路径查找目录 */
export function findDirByPath(bucketName: string, path: string): Promise<Dir | null> {
  return get<Dir | null>(`/member/dir/${bucketName}/findByPath`, { params: { path } });
}

/** 新建目录 */
export function addDir(bucketName: string, data: { path: string }) {
  return post<null>(`/member/dir/${bucketName}`, data);
}

/** 重命名目录 */
export function updateDir(bucketName: string, id: number, data: { path: string }) {
  return put<null>(`/member/dir/${bucketName}/${id}`, data);
}

/** 合并目录 */
export function mergeDir(bucketName: string, srcId: number, desId: number) {
  return put<null>(`/member/dir/${bucketName}/merge/${srcId}/to/${desId}`);
}

/* -------------------------------------------------------------- 上传 */

/**
 * 上传相关请求的超时时间（毫秒）。
 *
 * 全局默认超时只有 60s（见 `request.ts`），分片 100MB 时弱网下必然超时，
 * 合并（服务端 `wait=true` 会阻塞到存储节点合并完成）耗时更久。
 */
const UPLOAD_TIMEOUT = 30 * 60 * 1000;

/**
 * 上传初始化。
 *
 * 注意：`isPublic` 与 `cover` 在服务端是 `@RequestParam`，
 * 必须放在 query string 中，multipart 表单字段不会被识别。
 */
export async function initUpload(
  bucketName: string,
  params: {
    pathname: string;
    chunks: number;
    totalMd5: string;
    totalSize: number;
    contentType?: string;
    isPublic?: boolean;
    cover?: boolean;
    lastModified?: number;
  },
): Promise<UploadInitResult> {
  const { isPublic, cover, ...form } = params;
  const query = new URLSearchParams();
  query.set('isPublic', String(isPublic ?? false));
  query.set('cover', String(cover ?? false));
  const body = new URLSearchParams();
  Object.entries(form).forEach(([k, v]) => {
    if (v !== undefined && v !== null) body.set(k, String(v));
  });
  return post<UploadInitResult>(
    `/upload/${bucketName}/init?${query.toString()}`,
    body.toString(),
    {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      timeout: UPLOAD_TIMEOUT,
    },
  );
}

/** 上传单个分片（100MB，耗时长，超时放宽） */
export function uploadChunk(
  bucketName: string,
  formData: FormData,
): Promise<null> {
  return post<null>(`/upload/${bucketName}`, formData, {
    timeout: UPLOAD_TIMEOUT,
  });
}

/**
 * 合并分片。
 *
 * `isPublic` / `cover` / `updateMd5` / `wait` 为 `@RequestParam`，必须走 query。
 *
 * ⚠️ `bucketName` **只能传一次**（这里放 query，不要再放进 body）。
 * 服务端 `mergeFiles(String bucketName, ...)` 是普通参数，Spring 绑定时会把
 * query 与 body 里的同名参数合并成数组，再由 `ArrayToString` 拼成 `"a,b"`，
 * 于是服务端拿到的桶名变成 `"default,default"`，报
 * `bucket不存在:default,default,currentUserId:1`。
 * 参见 `NeedPermAspect` 的 `findBucketByUserIdAndBucketName`。
 */
export async function mergeUpload(
  params: {
    bucketName: string;
    pathname: string;
    totalMd5: string;
    totalSize: number;
    chunks?: number;
    lastModified?: number;
    contentType?: string;
    isPublic?: boolean;
    cover?: boolean;
    updateMd5?: boolean;
    wait?: boolean;
  },
): Promise<null> {
  const {
    bucketName,
    pathname,
    totalMd5,
    totalSize,
    chunks,
    lastModified,
    contentType,
    isPublic,
    cover,
    updateMd5,
    wait,
  } = params;
  const query = new URLSearchParams();
  query.set('bucketName', bucketName);
  query.set('isPublic', String(isPublic ?? false));
  query.set('cover', String(cover ?? false));
  query.set('updateMd5', String(updateMd5 ?? false));
  query.set('wait', String(wait ?? true));

  const body = new URLSearchParams();
  body.set('pathname', pathname);
  body.set('totalMd5', totalMd5);
  body.set('totalSize', String(totalSize));
  if (chunks !== undefined) body.set('chunks', String(chunks));
  if (lastModified !== undefined) body.set('lastModified', String(lastModified));
  if (contentType) body.set('contentType', contentType);
  // 注意：这里不要再放 bucketName，理由见上方注释

  return post<null>(`/upload/mergeFiles?${query.toString()}`, body.toString(), {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    // 服务端 wait=true 会阻塞到存储节点合并完成，耗时可能很长
    timeout: UPLOAD_TIMEOUT,
  });
}

/* -------------------------------------------------------------- 统计 */

/** 统计概览 */
export function fetchStatisticInfo(bucketName: string): Promise<StatisticInfo> {
  return get<StatisticInfo>(`/member/audit/statistic/info/${bucketName}`);
}

/** 图表数据：type ∈ request24Hours | flow24Hours | request30Days | flow30Days */
export function fetchChart(
  bucketName: string,
  type: string,
): Promise<ChartRequestData[] | ChartFlowData[]> {
  return get<ChartRequestData[] | ChartFlowData[]>(
    `/member/audit/chart/${bucketName}/${type}`,
  );
}

/* -------------------------------------------------------------- 管理 */

/** 用户分页列表 */
export function fetchUsers(params: {
  pageNum?: number;
  pageSize?: number;
}): Promise<PageInfo<User>> {
  return get<PageInfo<User>>('/admin/user/page/list', { params });
}

/** 新增用户 */
export function addUser(data: {
  name: string;
  username: string;
  password: string;
  isEnable: boolean;
  isAdmin: boolean;
}) {
  return post<User>('/admin/user', data);
}

/** 更新用户 */
export function updateUser(data: {
  id: number;
  name: string;
  username: string;
  password?: string;
  isEnable: boolean;
  isAdmin: boolean;
}) {
  return put<User>('/admin/user', data);
}

/** 解锁用户 */
export function unlockUser(username: string) {
  return put<null>(`/admin/user/unlock/${encodeURIComponent(username)}`);
}

/** 删除用户 */
export function deleteUser(id: number) {
  return del<number>(`/admin/user/${id}`);
}

/** 存储节点分页列表 */
export function fetchClients(params: {
  pageNum?: number;
  pageSize?: number;
}): Promise<PageInfo<Client>> {
  return get<PageInfo<Client>>('/admin/client/list', { params });
}

/** 剔除节点 */
export function kickClient(id: number) {
  return del<null>(`/admin/client/kick/${id}`);
}

/** 恢复节点 */
export function recoverClient(id: number) {
  return put<null>(`/admin/client/recover/${id}`);
}

/* ---------------------------------------------------------- 个人资料 */

/** 修改当前用户的姓名 / 密码 */
export function updateCurrentUser(data: {
  name?: string;
  currentPassword?: string;
  password?: string;
}) {
  return put<null>('/member/user', data);
}

export type { Bucket };
