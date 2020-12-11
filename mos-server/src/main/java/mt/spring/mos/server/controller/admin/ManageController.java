package mt.spring.mos.server.controller.admin;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import mt.common.entity.ResResult;
import mt.spring.mos.sdk.utils.Assert;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.entity.vo.BackVo;
import mt.spring.mos.server.service.FileHouseService;
import mt.spring.mos.server.service.ResourceService;
import mt.spring.mos.server.service.ServerJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.Future;

/**
 * @Author Martin
 * @Date 2020/5/20
 */
@RestController
@RequestMapping("/admin")
@Api(tags = "管理接口")
public class ManageController {
	@Autowired
	private FileHouseService fileHouseService;
	@Autowired
	private ServerJob serverJob;
	@Autowired
	private ResourceService resourceService;
	
	@GetMapping("/back")
	@ApiOperation("备份某个资源")
	public ResResult back(Long fileHouseId, Integer amount) {
		BackVo backVo = new BackVo();
		backVo.setFileHouseId(fileHouseId);
		backVo.setDataFragmentsAmount(amount);
		fileHouseService.backFileHouse(backVo);
		return ResResult.success();
	}
	
	@ApiOperation("备份所有资源")
	@GetMapping("/back/all")
	public ResResult back() {
		serverJob.checkBackFileHouse();
		return ResResult.success();
	}
	
	@ApiOperation("删除没有使用的文件")
	@DeleteMapping("/deleteNotUsedFile/{recentDays}")
	public ResResult deleteNotUsedFile(@PathVariable Integer recentDays) {
		serverJob.checkFileHouseAndDeleteRecent(recentDays, false);
		return ResResult.success();
	}
	
	@ApiOperation("生成截图")
	@PostMapping("/createThumb")
	public ResResult createThumb(Integer resourceId) throws Exception {
		Resource resource = resourceService.findById(resourceId);
		Assert.notNull(resource, "资源不能为空");
		Future<Boolean> result = resourceService.createThumb(resource);
		return ResResult.success(result.get());
	}
}
