package mt.spring.mos.server.controller.member;

import com.github.xiaoymin.knife4j.annotations.Ignore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import mt.common.annotation.CurrentUser;
import mt.common.entity.ResResult;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.User;
import mt.spring.mos.server.service.AuditService;
import mt.spring.mos.server.service.BucketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author Martin
 * @Date 2020/12/19
 */
@RestController
@RequestMapping("/member/audit")
@Tag(name = "统计")
public class AuditController {
	@Autowired
	private AuditService auditService;
	@Autowired
	private BucketService bucketService;
	
	@GetMapping("/statistic/info/{bucketName}")
	@Operation(summary = "获取统计信息")
	public ResResult getInfo(@Ignore @CurrentUser User currentUser, @PathVariable String bucketName) {
		Bucket currentBucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(currentBucket, "找不到bucket:" + bucketName);
		return ResResult.success(auditService.findStatisticInfo(currentBucket.getId()));
	}
	
	@GetMapping(value = {"/chart/{bucketName}/{type}"})
	@Operation(summary = "图表数据")
	public ResResult chartFlow(
			@PathVariable String type,
			@PathVariable String bucketName,
			@Ignore @CurrentUser User currentUser
	) {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "未拥有桶" + bucketName);
		Object result = null;
		switch (type) {
			case "request24Hours":
				result = auditService.find24HoursRequestList(bucket.getId());
				break;
			case "flow24Hours":
				result = auditService.find24HoursFlowList(bucket.getId());
				break;
			case "request30Days":
				result = auditService.find30DaysRequestList(bucket.getId());
				break;
			case "flow30Days":
				result = auditService.find30DaysFlowList(bucket.getId());
				break;
		}
		return ResResult.success(result);
	}
	
}
