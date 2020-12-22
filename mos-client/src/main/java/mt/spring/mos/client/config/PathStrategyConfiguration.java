package mt.spring.mos.client.config;

import mt.spring.mos.client.entity.MosClientProperties;
import mt.spring.mos.client.service.strategy.PathStrategy;
import mt.spring.mos.client.service.strategy.UploadPartStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author Martin
 * @Date 2020/12/21
 */
@Configuration
public class PathStrategyConfiguration {
	@Bean
	public PathStrategy pathStrategy(MosClientProperties mosClientProperties) {
		return new UploadPartStrategy(mosClientProperties);
	}
}
