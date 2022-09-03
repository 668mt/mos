package mt.spring.mos.server.config;

import mt.spring.mos.server.entity.MosServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    @Override
    public Executor getAsyncExecutor() {
        return Executors.newFixedThreadPool(mosServerProperties.getAsyncTaskThreadCore());
    }

    @Bean(name = DEFAULT_EXECUTOR_NAME)
    public ExecutorService defaultExecutor() {
        return Executors.newFixedThreadPool(5);
    }
}
