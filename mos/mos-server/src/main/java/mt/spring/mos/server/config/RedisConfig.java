package mt.spring.mos.server.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {
	@Bean
	@SuppressWarnings("all")
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(factory);
		Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
		ObjectMapper om = new ObjectMapper();
		om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
		jackson2JsonRedisSerializer.setObjectMapper(om);
		StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
		// key采用String的序列化方式
		template.setKeySerializer(stringRedisSerializer);
		// hash的key也采用String的序列化方式
		template.setHashKeySerializer(stringRedisSerializer);
		// value序列化方式采用jackson
		template.setValueSerializer(jackson2JsonRedisSerializer);
		// hash的value序列化方式采用jackson
		template.setHashValueSerializer(jackson2JsonRedisSerializer);
		template.afterPropertiesSet();
		return template;
	}

//	/**
//	 * <p>SpringBoot配置redis作为默认缓存工具</p>
//	 * <p>SpringBoot 2.0 以上版本的配置</p>
//	 */
//	@Bean
//	public CacheManager cacheManager(RedisTemplate<String, Object> template) {
//		RedisCacheConfiguration defaultCacheConfiguration =
//				RedisCacheConfiguration
//						.defaultCacheConfig().prefixKeysWith("oss-server")
//						// 设置key为String
//						.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(template.getStringSerializer()))
//						// 设置value 为自动转Json的Object
//						.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(template.getValueSerializer()))
//						// 不缓存null
//						.disableCachingNullValues()
//						// 缓存数据保存1小时
//						.entryTtl(Duration.ofHours(1));
//		return RedisCacheManager.RedisCacheManagerBuilder
//				// Redis 连接工厂
//				.fromConnectionFactory(template.getConnectionFactory())
//				// 缓存配置
//				.cacheDefaults(defaultCacheConfiguration)
//				.withInitialCacheConfigurations()
//				// 配置同步修改或删除 put/evict
//				.transactionAware()
//				.build();
//	}
	
	@Bean
	public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
		
		RedisSerializer<String> redisSerializer = new StringRedisSerializer();
		Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
		
		//解决查询缓存转换异常的问题
		ObjectMapper om = new ObjectMapper();
		om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
		jackson2JsonRedisSerializer.setObjectMapper(om);
		
		//配置序列化(解决乱码的问题)
		RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofHours(1))
				.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
				.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer))
				.disableCachingNullValues();
		
//		// 对每个缓存空间应用不同的配置
//		Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
//		configMap.put("userCache", config.entryTtl(Duration.ofHours(1)));
//		configMap.put("bucketCache", config.entryTtl(Duration.ofHours(1)));
		
		return RedisCacheManager.builder(factory)
				.cacheDefaults(config)
//				.initialCacheNames(configMap.keySet())// 注意这两句的调用顺序，一定要先调用该方法设置初始化的缓存名，再初始化相关的配置
//				.withInitialCacheConfigurations(configMap)
				.build();
	}
}