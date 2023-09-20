package mt.spring.mos.server.config;

import mt.common.config.log.TraceExecutor;
import mt.spring.mos.server.entity.MosServerProperties;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author Martin
 * @Date 2020/12/11
 */
@Configuration
@EnableAsync
public class AsyncConfiguration implements AsyncConfigurer {
	@Autowired
	private MosServerProperties mosServerProperties;
	public static final String DEFAULT_EXECUTOR_NAME = "defaultExecutor";
	public static final String META_EXECUTOR_NAME = "metaExecutor";
	
	@Override
	@Bean
	public Executor getAsyncExecutor() {
		return Executors.newFixedThreadPool(mosServerProperties.getAsyncTaskThreadCore());
	}
	
	@Bean(name = DEFAULT_EXECUTOR_NAME)
	public ExecutorService defaultExecutor() {
		return Executors.newFixedThreadPool(5);
	}
	
	@Bean(name = META_EXECUTOR_NAME)
	public ExecutorService thumbExecutor() {
		return new ThreadPoolExecutor(2, 2, 0L, TimeUnit.SECONDS, new LinkedBlockingDeque<>(), new ThreadFactory() {
			private final AtomicInteger number = new AtomicInteger(1);
			
			@Override
			public Thread newThread(@NotNull Runnable r) {
				Thread thread = new Thread(r);
				thread.setName("meta-thread-" + number.getAndIncrement());
				return thread;
			}
		});
	}
}
