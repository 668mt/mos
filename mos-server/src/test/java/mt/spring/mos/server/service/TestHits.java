package mt.spring.mos.server.service;

import mt.common.hits.HitsRecorder;
import mt.common.hits.HitsRecorderDownScheduler;
import mt.spring.mos.server.config.hits.HitsRecorderConfiguration;
import mt.spring.mos.server.config.hits.MosHitsRecorder;
import mt.spring.mos.server.config.hits.ResourceRedisTimeDownHandler;
import mt.spring.mos.server.config.hits.TimeHits;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2023/4/4
 */
public class TestHits {
	@Test
	public void test() throws IOException {
		LoggingSystem.get(HitsRecorderConfiguration.class.getClassLoader()).setLogLevel("root", LogLevel.INFO);
		File file = new File("D:\\work\\redissonClient-local.yaml");
		Config config = Config.fromYAML(file);
		RedissonClient redissonClient = Redisson.create(config);
		RedissonConnectionFactory redissonConnectionFactory = new RedissonConnectionFactory(redissonClient);
		HitsRecorderConfiguration hitsRecorderConfiguration = new HitsRecorderConfiguration();
		RedisTemplate<String, Object> redisTemplate = hitsRecorderConfiguration.redisTemplate(redissonConnectionFactory);
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
