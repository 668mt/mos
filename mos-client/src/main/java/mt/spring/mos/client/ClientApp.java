package mt.spring.mos.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Author Martin
 * @Date 2020/5/15
 */
@SpringBootApplication
@EnableScheduling
public class ClientApp {
	public static void main(String[] args) {
		SpringApplication.run(ClientApp.class, args);
	}
}
