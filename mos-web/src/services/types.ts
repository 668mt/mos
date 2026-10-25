/**
 * 后端 API 类型定义
 *
 * 契约来源：mos-server 的 `ResResult` 响应封装 + 各 Controller 的 DTO/VO。
 */

/** 全局响应封装：{ status, message, result, code } */
export interface ResResult<T = any> {
  status: 'ok' | 'error';
  message: string | null;
  result: T;
  code?: string | null;
}

/** PageInfo 分页结构（PageHelper） */
export interface PageInfo<T> {
  pageNum: number;
  pageSize: number;
  size: number;
  startRow: number;
  endRow: number;
  pages: number;
  total: number;
  prePage: number;
  nextPage: number;
  isFirstPage: boolean;
  isLastPage: boolean;
  hasPreviousPage: boolean;
  hasNextPage: boolean;
  navigatePages: number;
  navigatepageNums: number[];
  navigateFirstPage: number;
  navigateLastPage: number;
  list: T[];
}

/** 桶权限枚举 */
export type BucketPerm = 'SELECT' | 'INSERT' | 'UPDATE' | 'DELETE';

/** 用户 */
export interface User {
  id: number;
  name: string;
  username: string;
  password?: string;
  isEnable: boolean | null;
  isAdmin: boolean;
  failures?: number;
  locked?: boolean;
  lastLoginDate?: string | null;
  loginTimes?: number;
  createdDate?: string;
  createdBy?: string | null;
  updatedDate?: string;
  updatedBy?: string | null;
}

/** 当前登录用户（localStorage.currentUser） */
export interface CurrentUser extends User {
  isAdmin: boolean;
}

/** 桶 */
export interface Bucket {
  id: number;
  bucketName: string;
  defaultIsPublic: boolean;
  userId: number;
  dataFragmentsAmount: number;
  createdDate?: string;
  createdBy?: string | null;
  updatedDate?: string;
  updatedBy?: string | null;
}

/** 桶列表项（额外含 isOwn / owner） */
export interface BucketVo extends Bucket {
  isOwn: boolean;
  owner: string;
}

/** 桶授权项 */
export interface BucketPermVo {
  bucketName: string;
  perms: BucketPerm[];
}

/** 用户授权项 */
export interface UserGrantVo extends User {
  key: string;
  title: string;
  perms: BucketPerm[] | null;
}

/** 桶授权详情 */
export interface BucketGrantResult {
  allUsers: UserGrantVo[];
  userKeys: string[];
}

/** 桶授权请求体 */
export interface BucketGrantDto {
  bucketId: number;
  grants: { userId: number; perms: BucketPerm[] }[];
}

/** 访问控制（秘钥） */
export interface AccessControl {
  openId: number;
  secretKey: string;
  bucketId: number;
  useInfo: string;
  userId: number;
  bucketName?: string;
  createdDate?: string;
  createdBy?: string | null;
  updatedDate?: string;
  updatedBy?: string | null;
}

/** 目录 */
export interface Dir {
  id: number;
  path: string;
  parentId: number | null;
  bucketId: number | null;
  isDelete?: boolean;
  deleteTime?: string | null;
  createdDate?: string;
  createdBy?: string | null;
  updatedDate?: string;
  updatedBy?: string | null;
  urlEncodePath?: string;
  name?: string;
}

/** 资源列表项（目录 + 文件统一结构） */
export interface DirAndResourceVo {
  isDir: boolean;
  id: number;
  path: string;
  sizeByte: number | null;
  createdDate?: string;
  createdBy?: string | null;
  updatedDate?: string;
  updatedBy?: string | null;
  /** 图标标识，如 icon-tupian2 */
  icon?: string | null;
  isPublic: boolean;
  contentType?: string | null;
  thumbFileHouseId?: number | null;
  visits?: number | null;
  lastModified?: number | null;
  signUrl?: string | null;
  rowNum?: number | null;
  during?: number | null;
  videoLength?: string | null;
  /** 服务端按路径分段编码后的路径 */
  urlEncodePath: string;
  /** 文件名（path 的 basename） */
  fileName: string;
  /** 可读大小，如 "12.06KB" */
  readableSize: string;
  /** 是否是图片 */
  image: boolean;
}

/** 资源列表响应体 */
export interface ResourceListResult {
  bucketName: string;
  resources: PageInfo<DirAndResourceVo>;
  currentDir: Dir;
  lastDir: Dir | null;
  parentDirs: Dir[];
}

/** 目录详情（含缩略图） */
export interface DirDetailInfo {
  thumbs: ResourceVo[];
  dirCount: number;
  fileCount: number;
}

/** 资源（含签名地址） */
export interface ResourceVo {
  id: number;
  name: string;
  contentType?: string | null;
  sizeByte?: number | null;
  dirId?: number | null;
  isPublic?: boolean;
  fileHouseId?: number | null;
  thumbFileHouseId?: number | null;
  suffix?: string | null;
  visits?: number | null;
  lastModified?: number | null;
  videoLength?: string | null;
  during?: number | null;
  createdDate?: string;
  createdBy?: string | null;
  updatedDate?: string;
  updatedBy?: string | null;
  signUrl?: string | null;
}

/** 文件后缀映射，如 { video: ['mp4','flv'], image: ['jpg'] } */
export interface FileSuffixMap {
  [type: string]: string[];
}

/** 统计信息 */
export interface StatisticInfo {
  thisDayReadBytes: number;
  thisDayWriteBytes: number;
  thisDayReadRequests: number;
  thisDayWriteRequests: number;
  thisMonthReadBytes: number;
  thisMonthWriteBytes: number;
  thisMonthReadRequests: number;
  thisMonthWriteRequests: number;
  readableThisDayRead: string;
  readableThisDayWrite: string;
  readableThisMonthRead: string;
  readableThisMonthWrite: string;
}

/** 请求趋势图表数据 */
export interface ChartRequestData {
  time: string;
  readRequests: number;
  writeRequests: number;
}

/** 流量趋势图表数据 */
export interface ChartFlowData {
  time: string;
  readMb: number;
  writeMb: number;
}

/** 存储节点 */
export interface Client {
  id: number;
  name: string;
  ip: string;
  port: number;
  remark?: string | null;
  weight: number;
  totalStorageByte: number;
  usedStorageByte: number;
  totalStorageGb: number;
  usedStorageGb: number;
  usedPercent: number;
  status: 'UP' | 'DOWN' | 'KICKED';
  lastBeatTime?: string;
  keepSpaceByte: number;
  priority_min?: number;
  priority_max?: number;
  url?: string;
  createdDate?: string;
  createdBy?: string | null;
  updatedDate?: string;
  updatedBy?: string | null;
}

/** 上传初始化结果 */
export interface UploadInitResult {
  fileExists: boolean;
  existedChunkIndexs?: number[];
  fileHouse?: unknown;
}

/** 重名检查结果 */
export interface CheckFileExistsResult {
  checkResults: Record<string, boolean>;
}
