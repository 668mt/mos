package mt.spring.mos.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author Martin
 * @Date 2021/1/30
 */
@RestController
@RequestMapping("/health")
public class HealthCheckController {
	@GetMapping
	public String getHealth() {
		return "UP";
	}
}
