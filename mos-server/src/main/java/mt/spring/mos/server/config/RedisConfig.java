package mt.spring.mos.server.config;

import mt.common.annotation.EnableRedisConfiguration;
import mt.common.fragment.RedisTaskFragment;
import mt.common.fragment.TaskFragment;
import mt.spring.mos.server.entity.MosServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RedisConfig extends CachingConfigurerSupport {
//	@Bean
//	@SuppressWarnings("all")
//	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
//		RedisTemplate<String, Object> template = new RedisTemplate<>();
//		template.setConnectionFactory(factory);
//		Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
//		ObjectMapper om = new ObjectMapper();
//		om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//		om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
//		jackson2JsonRedisSerializer.setObjectMapper(om);
//		StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
//		// key采用String的序列化方式
//		template.setKeySerializer(stringRedisSerializer);
//		// hash的key也采用String的序列化方式
//		template.setHashKeySerializer(stringRedisSerializer);
//		// value序列化方式采用jackson
//		template.setValueSerializer(jackson2JsonRedisSerializer);
//		// hash的value序列化方式采用jackson
//		template.setHashValueSerializer(jackson2JsonRedisSerializer);
//		template.afterPropertiesSet();
//		return template;
//	}
//
//	@Override
//	public KeyGenerator keyGenerator() {
//		return (target, method, params) -> {
//			String path = target.getClass().getName() + "." + method.getName();
//			for (Object param : params) {
//				path += ":" + param;
//			}
//			return path;
//		};
//	}
//
//	@Bean
//	public RedisCacheManager cacheManager(MosServerProperties mosServerProperties, RedisConnectionFactory factory) {
//
//		RedisSerializer<String> redisSerializer = new StringRedisSerializer();
//		Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
//
//		//解决查询缓存转换异常的问题
//		ObjectMapper om = new ObjectMapper();
//		om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//		om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
//		jackson2JsonRedisSerializer.setObjectMapper(om);
//
//		//配置序列化(解决乱码的问题)
//		RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
//				.entryTtl(Duration.ofHours(1))
//				.prefixKeysWith(mosServerProperties.getRedisPrefix() + ":")
//				.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
//				.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer))
////				.disableCachingNullValues()
//				;
//
//		// 对每个缓存空间应用不同的配置
//		Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
//		configMap.put("permCache", config.entryTtl(Duration.ofDays(7)));
//		configMap.put("bucketCache", config.entryTtl(Duration.ofDays(7)));
//		configMap.put("accessControlCache", config.entryTtl(Duration.ofDays(7)));
//		configMap.put("statisticHourCache", config.entryTtl(Duration.ofHours(2)));
//		configMap.put("statisticDayCache", config.entryTtl(Duration.ofDays(1)));
//
//		return RedisCacheManager.builder(factory)
//				.cacheDefaults(config)
//				.initialCacheNames(configMap.keySet())// 注意这两句的调用顺序，一定要先调用该方法设置初始化的缓存名，再初始化相关的配置
//				.withInitialCacheConfigurations(configMap)
//				.build();
//	}
	
	@Value("${spring.application.name:default}")
	private String applicationName;
	@Autowired
	private ServerProperties serverProperties;
	
	@Bean
	public TaskFragment redisTaskFragment(RedisTemplate<String, Object> redisTemplate, MosServerProperties mosServerProperties) {
		Integer port = serverProperties.getPort();
		return new RedisTaskFragment(applicationName + ":" + mosServerProperties.getRedisPrefix(), redisTemplate, RedisTaskFragment.getHostIp(null) + ":" + port);
	}
}