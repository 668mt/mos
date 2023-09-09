package mt.spring.mos.server.service;

import lombok.extern.slf4j.Slf4j;
import mt.utils.common.TimeWatcher;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @Author Martin
 * @Date 2023/9/5
 */
@Slf4j
public class TestDirService extends BaseSpringBootTest {
	@Autowired
	private DirService dirService;
	
	@Test
	public void testAddDirs() throws Exception {
		dirService.realDeleteDir(bucket.getId(), "/test");
		ExecutorService executorService = Executors.newFixedThreadPool(100);
		TimeWatcher timeWatcher = new TimeWatcher();
		timeWatcher.start();
		try {
			List<? extends Future<?>> futures = IntStream.range(0, 2000).mapToObj(index -> executorService.submit(() -> {
				String path = "/test/202309/" + Thread.currentThread().getName() + "/" + index;
				dirService.addDir(path, bucket.getId());
			})).collect(Collectors.toList());
			for (Future<?> future : futures) {
				try {
					future.get();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		} finally {
			timeWatcher.recordFromStart("测试完成");
		}
	}
}
