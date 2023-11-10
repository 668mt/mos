package mt.spring.mos.server.config;

import mt.common.fragment.RedisTaskFragment;
import mt.common.fragment.TaskFragment;
import mt.spring.mos.server.entity.MosServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RedisConfig {
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