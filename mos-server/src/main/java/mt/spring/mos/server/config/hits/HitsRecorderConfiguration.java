package mt.spring.mos.server.config.hits;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import mt.common.hits.HitsRecorder;
import mt.common.hits.HitsRecorderDownScheduler;
import mt.common.hits.LocalHitsRecorder;
import mt.spring.mos.server.entity.MosServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2023/4/2
 */
@Configuration
public class HitsRecorderConfiguration {
	@Autowired
	private MosServerProperties mosServerProperties;
	
	@Bean
	public HitsRecorderDownScheduler hitsRecorderDownScheduler(List<HitsRecorder<?, ?>> hitsRecorderList) {
		HitsRecorderDownScheduler hitsRecorderDownScheduler = new HitsRecorderDownScheduler(hitsRecorderList);
		hitsRecorderDownScheduler.start(10, TimeUnit.MINUTES);
		return hitsRecorderDownScheduler;
	}
	
	@Bean
	public HitsRecorder<String, Long> resourceVisitsRecorder(ResourceVisitsDownHandler resourceVisitsDownHandler) {
		return new LocalHitsRecorder<>(resourceVisitsDownHandler);
	}
	
	@Bean
	public MosHitsRecorder hourReadBytesRecorder(RedisTemplate<String, Object> redisTemplate) {
		String key = mosServerProperties.getRedisPrefix() + ":hits:hour:read:bytes";
		String pattern = "yyyyMMddHH";
		long removeTimeMills = 2 * 24 * 60 * 60 * 1000L;
		ResourceRedisTimeDownHandler resourceRedisTimeDownHandler = new ResourceRedisTimeDownHandler(redisTemplate, key, pattern, removeTimeMills);
		return new MosHitsRecorder(resourceRedisTimeDownHandler);
	}
	
	@Bean
	public MosHitsRecorder hourWriteBytesRecorder(RedisTemplate<String, Object> redisTemplate) {
		String key = mosServerProperties.getRedisPrefix() + ":hits:hour:write:bytes";
		String pattern = "yyyyMMddHH";
		long removeTimeMills = 2 * 24 * 60 * 60 * 1000L;
		ResourceRedisTimeDownHandler resourceRedisTimeDownHandler = new ResourceRedisTimeDownHandler(redisTemplate, key, pattern, removeTimeMills);
		return new MosHitsRecorder(resourceRedisTimeDownHandler);
	}
	
	@Bean
	public MosHitsRecorder hourReadRequestsRecorder(RedisTemplate<String, Object> redisTemplate) {
		String key = mosServerProperties.getRedisPrefix() + ":hits:hour:read:requests";
		String pattern = "yyyyMMddHH";
		long removeTimeMills = 2 * 24 * 60 * 60 * 1000L;
		ResourceRedisTimeDownHandler resourceRedisTimeDownHandler = new ResourceRedisTimeDownHandler(redisTemplate, key, pattern, removeTimeMills);
		return new MosHitsRecorder(resourceRedisTimeDownHandler);
	}
	
	
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
	
	@Bean
	public MosHitsRecorder hourWriteRequestsRecorder(RedisTemplate<String, Object> redisTemplate) {
		String key = mosServerProperties.getRedisPrefix() + ":hits:hour:write:requests";
		String pattern = "yyyyMMddHH";
		long removeTimeMills = 2 * 24 * 60 * 60 * 1000L;
		ResourceRedisTimeDownHandler resourceRedisTimeDownHandler = new ResourceRedisTimeDownHandler(redisTemplate, key, pattern, removeTimeMills);
		return new MosHitsRecorder(resourceRedisTimeDownHandler);
	}
	
	@Bean
	public MosHitsRecorder dayReadBytesRecorder(RedisTemplate<String, Object> redisTemplate) {
		String key = mosServerProperties.getRedisPrefix() + ":hits:day:read:bytes";
		String pattern = "yyyyMMdd";
		long removeTimeMills = 2 * 30 * 24 * 60 * 60 * 1000L;
		ResourceRedisTimeDownHandler resourceRedisTimeDownHandler = new ResourceRedisTimeDownHandler(redisTemplate, key, pattern, removeTimeMills);
		return new MosHitsRecorder(resourceRedisTimeDownHandler);
	}
	
	@Bean
	public MosHitsRecorder dayWriteBytesRecorder(RedisTemplate<String, Object> redisTemplate) {
		String key = mosServerProperties.getRedisPrefix() + ":hits:day:write:bytes";
		String pattern = "yyyyMMdd";
		long removeTimeMills = 2 * 30 * 24 * 60 * 60 * 1000L;
		ResourceRedisTimeDownHandler resourceRedisTimeDownHandler = new ResourceRedisTimeDownHandler(redisTemplate, key, pattern, removeTimeMills);
		return new MosHitsRecorder(resourceRedisTimeDownHandler);
	}
	
	@Bean
	public MosHitsRecorder dayReadRequestsRecorder(RedisTemplate<String, Object> redisTemplate) {
		String key = mosServerProperties.getRedisPrefix() + ":hits:day:read:requests";
		String pattern = "yyyyMMdd";
		long removeTimeMills = 2 * 30 * 24 * 60 * 60 * 1000L;
		ResourceRedisTimeDownHandler resourceRedisTimeDownHandler = new ResourceRedisTimeDownHandler(redisTemplate, key, pattern, removeTimeMills);
		return new MosHitsRecorder(resourceRedisTimeDownHandler);
	}
	
	@Bean
	public MosHitsRecorder dayWriteRequestsRecorder(RedisTemplate<String, Object> redisTemplate) {
		String key = mosServerProperties.getRedisPrefix() + ":hits:day:write:requests";
		String pattern = "yyyyMMdd";
		long removeTimeMills = 2 * 30 * 24 * 60 * 60 * 1000L;
		ResourceRedisTimeDownHandler resourceRedisTimeDownHandler = new ResourceRedisTimeDownHandler(redisTemplate, key, pattern, removeTimeMills);
		return new MosHitsRecorder(resourceRedisTimeDownHandler);
	}
}
