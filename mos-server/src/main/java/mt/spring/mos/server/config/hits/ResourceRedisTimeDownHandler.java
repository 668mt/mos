package mt.spring.mos.server.config.hits;

import lombok.extern.slf4j.Slf4j;
import mt.common.hits.HitsDownHandler;
import mt.spring.mos.base.utils.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Author Martin
 * @Date 2023/4/2
 */
@Slf4j
public class ResourceRedisTimeDownHandler implements HitsDownHandler<Long, String> {
	private final RedisTemplate<String, Object> redisTemplate;
	private final String key;
	private final String pattern;
	private final long removeTimeMills;
	
	public ResourceRedisTimeDownHandler(@NotNull RedisTemplate<String, Object> redisTemplate, @NotNull String key, @NotNull String pattern, long removeTimeMills) {
		this.redisTemplate = redisTemplate;
		this.key = key;
		this.pattern = pattern;
		this.removeTimeMills = removeTimeMills;
	}
	
	public String getKey(long bucketId) {
		return this.key + ":bucket-" + bucketId;
	}
	
	public List<TimeHits> getData(long bucketId, @Nullable Date beginTime, @Nullable Date endTime) {
		List<TimeHits> results = new ArrayList<>();
		ZSetOperations<String, Object> zSetOperations = redisTemplate.opsForZSet();
		String key = getKey(bucketId);
		//移除过期数据
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		Long size = zSetOperations.size(key);
		if (size != null && size > 0) {
			Set<ZSetOperations.TypedTuple<Object>> list = zSetOperations.rangeWithScores(key, 0, size);
			if (CollectionUtils.isNotEmpty(list)) {
				for (ZSetOperations.TypedTuple<Object> typedTuple : list) {
					if (typedTuple.getValue() == null) {
						continue;
					}
					String time = typedTuple.getValue().toString();
					Double score = typedTuple.getScore();
					if (score == null) {
						score = 0d;
					}
					try {
						Date date = simpleDateFormat.parse(time);
						if (beginTime != null && date.getTime() < beginTime.getTime()) {
							continue;
						}
						if (endTime != null && date.getTime() > endTime.getTime()) {
							continue;
						}
						results.add(new TimeHits(time, score.longValue(), date));
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}
			}
		}
		//时间从小到大排序
		results.sort(Comparator.comparing(TimeHits::getDate));
		return results;
	}
	
	public long getTotal(long bucketId, @Nullable Date beginTime, @Nullable Date endTime) {
		long total = 0;
		List<TimeHits> timeHits = getData(bucketId, beginTime, endTime);
		for (TimeHits timeHit : timeHits) {
			total += timeHit.getHits();
		}
		return total;
	}
	
	/**
	 * key，格式yyyyMMddHH
	 *
	 * @param hitsMap key为资源id，value为命中次数
	 */
	@Override
	public void doHitsDown(@Nullable Long bucketId, @NotNull Map<String, Long> hitsMap) {
		if (bucketId == null) {
			return;
		}
		//redis zSet value为时间，score为小时内的字节数
		ZSetOperations<String, Object> zSetOperations = redisTemplate.opsForZSet();
		String key = getKey(bucketId);
		//移除过期数据
		List<TimeHits> list = getData(bucketId, null, null);
		if (CollectionUtils.isNotEmpty(list)) {
			for (TimeHits timeHits : list) {
				if (timeHits.getDate().getTime() < System.currentTimeMillis() - removeTimeMills) {
					zSetOperations.remove(key, timeHits.getTime());
					log.debug("移除过期数据：{}", timeHits.getTime());
				}
			}
		}
		//新增数据
		for (Map.Entry<String, Long> stringLongEntry : hitsMap.entrySet()) {
			String time = stringLongEntry.getKey();
			Long bytes = stringLongEntry.getValue();
			zSetOperations.incrementScore(key, time, bytes);
		}
	}
}
