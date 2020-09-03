package mt.spring.mos.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import tk.mybatis.spring.annotation.MapperScan;

/**
 * @Author Martin
 * @Date 2020/5/15
 */
@SpringBootApplication
@EnableEurekaClient
@EnableScheduling
@MapperScan("mt.spring.mos.server.dao")
@EnableAsync
@EnableRedisHttpSession
public class ServerApp {
	public static void main(String[] args) {
		SpringApplication.run(ServerApp.class, args);
	}
}
