package mt.spring.mos.server.controller.admin;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import mt.common.entity.ResResult;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Dir;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.entity.vo.BackVo;
import mt.spring.mos.server.service.BucketService;
import mt.spring.mos.server.service.DirService;
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
	private DirService dirService;
	@Autowired
	private ServerJob serverJob;
	@Autowired
	private BucketService bucketService;
	
	@GetMapping("/back")
	@ApiOperation("备份某个资源")
	public ResResult back(Long resourceId) {
		Resource resource = resourceService.findById(resourceId);
		Dir dir = dirService.findById(resource.getDirId());
		Bucket bucket = bucketService.findById(dir.getBucketId());
		BackVo backVo = new BackVo();
		backVo.setResourceId(resourceId);
		backVo.setDataFragmentsAmount(bucket.getDataFragmentsAmount() == null ? 1 : bucket.getDataFragmentsAmount());
		resourceService.backResource(backVo);
		return ResResult.success();
	}
	
	@ApiOperation("备份所有资源")
	@GetMapping("/back/all")
	public ResResult back() {
		serverJob.checkBackResource();
		return ResResult.success();
	}
}
