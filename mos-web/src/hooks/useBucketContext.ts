import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from '@umijs/max';
import { fetchBuckets } from '@/services/api';
import type { BucketVo } from '@/services/types';

/** URL 查询参数：桶名。桶内三个页签共用 */
export const BUCKET_PARAM = 'b';

/**
 * 当前桶上下文。
 *
 * 桶名放在 URL 查询参数 `b` 里（而不是路径段），
 * 这样「仪表盘 / 资源管理 / 回收站」三个页签共用同一个参数，
 * 切换页签时桶不会丢。
 */
export function useBucketContext() {
  const [searchParams] = useSearchParams();
  const [buckets, setBuckets] = useState<BucketVo[]>([]);
  const [loading, setLoading] = useState(false);

  const reload = useCallback(async () => {
    setLoading(true);
    try {
      const list = await fetchBuckets();
      setBuckets(list || []);
    } catch {
      // 错误已由拦截器处理
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  const paramBucket = searchParams.get(BUCKET_PARAM) || undefined;

  /**
   * 生效的桶：URL 指定的桶优先；URL 没带或桶已不存在时回退到第一个桶。
   */
  const currentBucket = useMemo(() => {
    if (paramBucket && buckets.some((b) => b.bucketName === paramBucket)) {
      return paramBucket;
    }
    return buckets[0]?.bucketName;
  }, [paramBucket, buckets]);

  const currentBucketVo = useMemo(
    () => buckets.find((b) => b.bucketName === currentBucket),
    [buckets, currentBucket],
  );

  return {
    buckets,
    loading,
    reload,
    /** URL 里显式指定的桶（可能已失效） */
    paramBucket,
    /** 实际生效的桶 */
    currentBucket,
    currentBucketVo,
  };
}
