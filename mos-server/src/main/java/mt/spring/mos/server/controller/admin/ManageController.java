package mt.spring.mos.server.controller.admin;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import mt.common.entity.ResResult;
import mt.spring.mos.server.service.ResourceService;
import mt.spring.mos.server.service.ServerJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author Martin
 * @Date 2020/5/20
 */
@RestController
@RequestMapping("/admin")
@Api(tags = "管理接口")
public class ManageController {
	@Autowired
	private ResourceService resourceService;
	@Autowired
	private ServerJob serverJob;
	
	@GetMapping("/back")
	@ApiOperation("备份某个资源")
	public ResResult back(Long resourceId) {
		resourceService.backResource(resourceId);
		return ResResult.success();
	}
	
	@ApiOperation("备份所有资源")
	@GetMapping("/back/all")
	public ResResult back() {
		serverJob.checkBackResource();
		return ResResult.success();
	}
}
