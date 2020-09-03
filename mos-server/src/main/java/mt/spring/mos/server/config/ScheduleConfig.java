package mt.spring.mos.server.config;

import mt.spring.mos.server.service.TaskScheduleService;
import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.Inet4Address;

/**
 * @Author Martin
 * @Date 2020/5/30
 */
@Configuration
public class ScheduleConfig {
	
	@SneakyThrows
	@Bean
	public TaskScheduleService taskScheduleService(RedisUtils redisUtils, ServerProperties serverProperties) {
		String host = Inet4Address.getLocalHost().getHostAddress() + ":" + serverProperties.getPort();
		String healthUrl = "http://" + host + "/actuator/info";
		return new TaskScheduleService("mosServerSchedule", host, healthUrl, redisUtils);
	}
}
