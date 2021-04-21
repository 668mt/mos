package mt.spring.mos.server.config;

import lombok.SneakyThrows;
import mt.spring.mos.base.utils.IpUtils;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.service.TaskScheduleService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @Author Martin
 * @Date 2020/5/30
 */
@Configuration
public class ScheduleConfig {
	@Value("${mos.schedule.name:mosServerSchedule}")
	private String scheduleName;
	@Value("${management.server.port}")
	private Integer port;
	
	@SneakyThrows
	@Bean
	public TaskScheduleService taskScheduleService(MosServerProperties mosServerProperties, RedisUtils redisUtils) {
		String host;
		if (StringUtils.isNotBlank(mosServerProperties.getCurrentIp())) {
			host = mosServerProperties.getCurrentIp() + ":" + port;
		} else {
			host = IpUtils.getHostIp(mosServerProperties.getIpPrefix()) + ":" + port;
		}
		String healthUrl = "http://" + host + "/actuator/info";
		return new TaskScheduleService(scheduleName, host, healthUrl, redisUtils);
	}
	
	public static void main(String[] args) {
		String hostIp = IpUtils.getHostIp("192.168.0");
		System.out.println(hostIp);
	}
	
	@Bean
	public TaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
		threadPoolTaskScheduler.setPoolSize(10);
		return threadPoolTaskScheduler;
	}
}
