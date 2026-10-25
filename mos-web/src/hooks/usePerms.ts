import { useCallback, useEffect, useMemo, useState } from 'react';
import { useModel } from '@umijs/max';
import { fetchOwnPerms } from '@/services/api';
import type { BucketPerm } from '@/services/types';

/**
 * 桶权限判断。
 *
 * 服务端 `GET /member/bucket/grant/perms/own` 返回
 * `[{ bucketName, perms: ['SELECT','INSERT',...] }]`，
 * 桶所有者拥有全部权限。
 */
export function usePerms() {
  const { initialState } = useModel('@@initialState') as any;
  const [perms, setPerms] = useState<Record<string, BucketPerm[]>>({});
  const [loading, setLoading] = useState(false);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const list = await fetchOwnPerms();
      const map: Record<string, BucketPerm[]> = {};
      (list || []).forEach((vo) => {
        map[vo.bucketName] = vo.perms || [];
      });
      setPerms(map);
    } catch {
      // 错误提示已在拦截器中处理
    } finally {
      setLoading(false);
    }
  }, []);

  const currentUser = initialState?.currentUser;

  useEffect(() => {
    if (currentUser) {
      refresh();
    }
  }, [currentUser, refresh]);

  const hasPerm = useCallback(
    (bucketName: string | undefined, perm: BucketPerm | string): boolean => {
      if (!bucketName) return false;
      const own = perms[bucketName];
      if (!own) return false;
      return own.some((p) => p.toUpperCase() === perm.toUpperCase());
    },
    [perms],
  );

  return useMemo(
    () => ({ perms, hasPerm, refresh, loading }),
    [perms, hasPerm, refresh, loading],
  );
}
