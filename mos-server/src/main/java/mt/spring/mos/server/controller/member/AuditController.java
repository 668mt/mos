package mt.spring.mos.server.controller.member;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import mt.common.annotation.CurrentUser;
import mt.common.entity.ResResult;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.User;
import mt.spring.mos.server.entity.vo.audit.ChartBy;
import mt.spring.mos.server.service.AuditService;
import mt.spring.mos.server.service.BucketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @Author Martin
 * @Date 2020/12/19
 */
@RestController
@RequestMapping("/member/audit")
@Api(tags = "统计")
public class AuditController {
	@Autowired
	private AuditService auditService;
	@Autowired
	private BucketService bucketService;
	
	@GetMapping("/statistic/info/{bucketName}")
	@ApiOperation("获取统计信息")
	public ResResult getInfo(@ApiIgnore @CurrentUser User currentUser, @PathVariable String bucketName) {
		Bucket currentBucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(currentBucket, "找不到bucket:" + bucketName);
		return ResResult.success(auditService.findStatisticInfoFromCache(currentBucket.getId()));
	}

	@GetMapping(value = {"/chart/{bucketName}/{type}"})
	@ApiOperation("图表数据")
	public ResResult chartFlow(
			@PathVariable String type,
			@PathVariable String bucketName,
			@ApiIgnore @CurrentUser User currentUser
	) {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "未拥有桶" + bucketName);
		Object result = null;
		switch (type) {
			case "request24Hours":
				result = auditService.find24HoursRequestListFromCache(bucket.getId());
				break;
			case "flow24Hours":
				result = auditService.find24HoursFlowListFromCache(bucket.getId());
				break;
			case "request30Days":
				result = auditService.find30DaysRequestListFromCache(bucket.getId());
				break;
			case "flow30Days":
				result = auditService.find30DaysFlowListFromCache(bucket.getId());
				break;
		}
		return ResResult.success(result);
	}
	
}
