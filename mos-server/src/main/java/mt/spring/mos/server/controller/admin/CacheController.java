package mt.spring.mos.server.controller.admin;

import mt.common.entity.ResResult;
import mt.spring.mos.server.service.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author Martin
 * @Date 2021/1/7
 */
@RestController
@RequestMapping("/admin/cache")
public class CacheController {
	@Autowired
	private CacheService cacheService;
	
	@GetMapping("/clearAll")
	public ResResult clearAll() {
		cacheService.clearAll();
		return ResResult.success();
	}
}
