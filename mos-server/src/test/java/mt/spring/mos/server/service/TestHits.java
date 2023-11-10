package mt.spring.mos.server.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import mt.common.hits.HitsRecorder;
import mt.common.hits.HitsRecorderDownScheduler;
import mt.spring.mos.server.config.hits.HitsRecorderConfiguration;
import mt.spring.mos.server.config.hits.MosHitsRecorder;
import mt.spring.mos.server.config.hits.ResourceRedisTimeDownHandler;
import mt.spring.mos.server.config.hits.TimeHits;
import org.junit.Before;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2023/4/4
 */
public class TestHits {
	private RedisTemplate<String, Object> redisTemplate;
	
	@Before
	public void before() throws IOException {
		LoggingSystem.get(HitsRecorderConfiguration.class.getClassLoader()).setLogLevel("root", LogLevel.INFO);
		File file = new File("D:\\work\\redissonClient-local.yaml");
		Config config = Config.fromYAML(file);
		RedissonClient redissonClient = Redisson.create(config);
		RedissonConnectionFactory redissonConnectionFactory = new RedissonConnectionFactory(redissonClient);
		redisTemplate = redisTemplate(redissonConnectionFactory);
		redisTemplate.setConnectionFactory(redissonConnectionFactory);
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
	
	@Test
	public void clearKeys() throws IOException {
		Set<String> keys = redisTemplate.keys("*ws-notice-key-*");
		redisTemplate.delete(keys);
	}
	
	@Test
	public void test() throws IOException {
		LoggingSystem.get(HitsRecorderConfiguration.class.getClassLoader()).setLogLevel("root", LogLevel.INFO);
		File file = new File("D:\\work\\redissonClient-local.yaml");
		Config config = Config.fromYAML(file);
		RedissonClient redissonClient = Redisson.create(config);
		RedissonConnectionFactory redissonConnectionFactory = new RedissonConnectionFactory(redissonClient);
		RedisTemplate<String, Object> redisTemplate = redisTemplate(redissonConnectionFactory);
		redisTemplate.setConnectionFactory(redissonConnectionFactory);
		
		String key = "test:hits:hour:read:requests";
		Objects.requireNonNull(redisTemplate.keys(key + "*")).forEach(redisTemplate::delete);
		String pattern = "yyyyMMddHH";
		long removeTimeMills = 2 * 24 * 60 * 60 * 1000L;
		ResourceRedisTimeDownHandler resourceRedisTimeDownHandler = new ResourceRedisTimeDownHandler(redisTemplate, key, pattern, removeTimeMills);
		MosHitsRecorder mosHitsRecorder = new MosHitsRecorder(resourceRedisTimeDownHandler);
		List<HitsRecorder<?, ?>> hitsRecorderList = new ArrayList<>();
		hitsRecorderList.add(mosHitsRecorder);
		HitsRecorderDownScheduler hitsRecorderDownScheduler = new HitsRecorderDownScheduler(hitsRecorderList);
		hitsRecorderDownScheduler.start(10, TimeUnit.MINUTES);
		
		Long bucketId = 1L;
		//2号
		mosHitsRecorder.recordHits(bucketId, "2023040201", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040201", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040201", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040202", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040202", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040203", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040203", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040203", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040204", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040204", 1L);
		//3号
		mosHitsRecorder.recordHits(bucketId, "2023040301", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040301", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040301", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040302", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040302", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040303", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040303", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040303", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040304", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040304", 1L);
		//4号
		mosHitsRecorder.recordHits(bucketId, "2023040401", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040401", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040401", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040402", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040402", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040403", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040403", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040403", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040404", 1L);
		mosHitsRecorder.recordHits(bucketId, "2023040404", 1L);
		
		hitsRecorderDownScheduler.hitsDown();
		Date beginDate = new Date(System.currentTimeMillis() - 24 * 3600 * 1000L);
		List<TimeHits> data = mosHitsRecorder.getHitsDownHandler().getData(bucketId, beginDate, null);
		for (TimeHits datum : data) {
			System.out.println(datum);
		}
		long total = mosHitsRecorder.getHitsDownHandler().getTotal(bucketId, beginDate, null);
		System.out.println(total);
	}
}
