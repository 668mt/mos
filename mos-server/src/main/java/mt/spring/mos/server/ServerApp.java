package mt.spring.mos.server;

import mt.common.annotation.EnableDataLock;
import mt.common.annotation.EnableRedisConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import tk.mybatis.spring.annotation.MapperScan;

/**
 * @Author Martin
 * @Date 2020/5/15
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("mt.spring.mos.server.dao")
@EnableRedisHttpSession(redisNamespace = "${spring.redis.session.namespace}")
@EnableCaching
@EnableDataLock
@EnableRedisConfiguration
public class ServerApp {
	public static void main(String[] args) {
		SpringApplication.run(ServerApp.class, args);
	}
}
