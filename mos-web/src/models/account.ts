import { fetchBuckets, fetchOwnPerms } from '@/services/api';
import type { BucketPerm, BucketPermVo, BucketVo, CurrentUser } from '@/services/types';

const STORAGE_KEY = 'currentUser';

/** 从 localStorage 读取当前登录用户 */
export function readCurrentUser(): CurrentUser | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    return parsed && parsed.id ? (parsed as CurrentUser) : null;
  } catch {
    return null;
  }
}

/** 写入当前登录用户 */
export function writeCurrentUser(user: CurrentUser | null) {
  if (user) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
  } else {
    localStorage.removeItem(STORAGE_KEY);
  }
}

/** Model state 类型 */
export interface AccountState {
  currentUser: CurrentUser | null;
  buckets: BucketVo[];
  currentBucket: string | undefined;
  perms: Record<string, BucketPerm[]>;
}

export default {
  state: {
    currentUser: readCurrentUser() as CurrentUser | null,
    /** 桶列表 */
    buckets: [] as BucketVo[],
    /** 当前选中的桶名 */
    currentBucket: undefined as string | undefined,
    /** bucketName -> 权限列表 */
    perms: {} as Record<string, BucketPerm[]>,
  } as AccountState,

  effects: {
    /** 登录成功后拉取桶列表与权限 */
    * loadWorkspace(_: unknown, { select }: any) {
      const buckets: BucketVo[] = yield fetchBuckets();
      const permsList: BucketPermVo[] = yield fetchOwnPerms();
      const perms: Record<string, BucketPerm[]> = {};
      permsList.forEach((vo) => {
        perms[vo.bucketName] = vo.perms || [];
      });
      const current: string | undefined = select((s: any) => s.account.currentBucket);
      const currentBucket =
        current && buckets.some((b) => b.bucketName === current)
          ? current
          : buckets[0]?.bucketName;
      return { buckets, perms, currentBucket };
    },

    /** 仅刷新权限 */
    * refreshPerms(_: unknown, { select }: any) {
      const permsList: BucketPermVo[] = yield fetchOwnPerms();
      const perms: Record<string, BucketPerm[]> = {};
      permsList.forEach((vo) => {
        perms[vo.bucketName] = vo.perms || [];
      });
      const currentBucket = select((s: any) => s.account.currentBucket);
      return { perms, currentBucket };
    },
  },

  reducers: {
    setCurrentUser(state: AccountState, payload: { currentUser: CurrentUser | null }) {
      state.currentUser = payload.currentUser;
      return { ...state };
    },
    setBuckets(state: AccountState, payload: { buckets: BucketVo[] }) {
      state.buckets = payload.buckets;
      return { ...state };
    },
    setCurrentBucket(state: AccountState, payload: { currentBucket: string | undefined }) {
      state.currentBucket = payload.currentBucket;
      return { ...state };
    },
    setPerms(state: AccountState, payload: { perms: Record<string, BucketPerm[]> }) {
      state.perms = payload.perms;
      return { ...state };
    },
    setWorkspace(
      state: AccountState,
      payload: {
        buckets: BucketVo[];
        perms: Record<string, BucketPerm[]>;
        currentBucket: string | undefined;
      },
    ) {
      state.buckets = payload.buckets;
      state.perms = payload.perms;
      state.currentBucket = payload.currentBucket;
      return { ...state };
    },
  },
};
