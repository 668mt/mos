package mt.spring.mos.server.controller.discovery;

import lombok.extern.slf4j.Slf4j;
import mt.common.entity.ResResult;
import mt.spring.mos.server.service.DiscoveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author Martin
 * @Date 2020/6/7
 */
@RestController
@RequestMapping("/discovery")
@Slf4j
public class DiscoveryController {
	
	@Autowired
	private DiscoveryService discoveryService;
	
	@PutMapping("/beat")
	public ResResult<?> register(@RequestParam(defaultValue = "false") Boolean isRegister, Instance instance) {
		discoveryService.register(isRegister, instance);
		return ResResult.success();
	}
	
}
