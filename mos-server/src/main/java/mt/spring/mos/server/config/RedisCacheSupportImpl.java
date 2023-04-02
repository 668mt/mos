package mt.spring.mos.server.config;

import mt.common.config.redis.RedisCacheSupport;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2023/4/2
 */
@Component
public class RedisCacheSupportImpl implements RedisCacheSupport {
	
	@Override
	public Map<String, RedisCacheConfiguration> getConfigurations(RedisCacheConfiguration config) {
		Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
		configMap.put("permCache", config.entryTtl(Duration.ofDays(7)));
		configMap.put("bucketCache", config.entryTtl(Duration.ofDays(7)));
		configMap.put("accessControlCache", config.entryTtl(Duration.ofDays(7)));
		configMap.put("statisticHourCache", config.entryTtl(Duration.ofHours(2)));
		configMap.put("statisticDayCache", config.entryTtl(Duration.ofDays(1)));
		return configMap;
	}
}
