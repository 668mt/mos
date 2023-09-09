package mt.spring.mos.server.service;

import mt.spring.mos.server.entity.po.Dir;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

/**
 * @Author Martin
 * @Date 2023/9/5
 */
public class TestMultiThreads extends BaseSpringBootTest {
	@Autowired
	private DirService dirService;
	@Autowired
	private BucketService bucketService;
	
	@Test
	public void testAddDirs() throws Exception {
		Dir dir = dirService.findOneByPathAndBucketId("/test/202309", bucket.getId(), null);
		if (dir != null) {
			dirService.deleteDir(bucket.getId(), dir.getId());
		}
		ExecutorService executorService = Executors.newFixedThreadPool(10);
		List<Future<?>> futures = new ArrayList<>();
		IntStream.range(0, 100).forEach(index -> {
			Future<?> future = executorService.submit(() -> {
				String path = "/test/202309/" + index;
				dirService.addDir(path, bucket.getId());
			});
			futures.add(future);
		});
		for (Future<?> future : futures) {
			future.get();
		}
	}
}
