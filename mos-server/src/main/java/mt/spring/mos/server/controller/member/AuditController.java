package mt.spring.mos.server.controller.member;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import mt.common.annotation.CurrentUser;
import mt.common.entity.ResResult;
import mt.spring.mos.sdk.utils.Assert;
import mt.spring.mos.server.entity.po.Audit;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.User;
import mt.spring.mos.server.entity.vo.audit.ChartBy;
import mt.spring.mos.server.entity.vo.audit.FlowStatisticVo;
import mt.spring.mos.server.entity.vo.audit.RequestStatisticVo;
import mt.spring.mos.server.service.AuditService;
import mt.spring.mos.server.service.BucketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

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
	
	@GetMapping("/statistic/info")
	@ApiOperation("获取统计信息")
	public ResResult getInfo(@ApiIgnore @CurrentUser User currentUser) {
		return ResResult.success(auditService.findStatisticInfo(currentUser.getId()));
	}
	
	@GetMapping("/statistic/flow/{type}/from/{startDate}")
	@ApiOperation("流量统计")
	public ResResult flowFromDate(@PathVariable String startDate, @PathVariable Audit.Type type, @ApiIgnore @CurrentUser User currentUser) {
		List<FlowStatisticVo> list = auditService.findFlowStatisticFrom(currentUser.getId(), type, startDate);
		return ResResult.success(list);
	}
	
	@GetMapping("/statistic/request/{type}/from/{startDate}")
	@ApiOperation("请求统计")
	public ResResult requestsFromDate(@PathVariable String startDate, @PathVariable Audit.Type type, @ApiIgnore @CurrentUser User currentUser) {
		List<RequestStatisticVo> list = auditService.findRequestStatisticFrom(currentUser.getId(), type, startDate);
		return ResResult.success(list);
	}
	
	@GetMapping(value = {"/chart/flow/{bucketName}/by/{by}"})
	@ApiOperation("流量图表数据")
	public ResResult chartFlow(String startDate,
							   @RequestParam(required = false) String endDate,
							   @PathVariable String bucketName,
							   @PathVariable ChartBy by,
							   @ApiIgnore @CurrentUser User currentUser
	) {
		Assert.notNull(startDate, "开始时间不能为空");
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "未拥有桶" + bucketName);
		return ResResult.success(auditService.findChartFlowList(bucket, startDate, endDate, by));
	}
	
	@GetMapping(value = {"/chart/request/{bucketName}/by/{by}"})
	@ApiOperation("请求图表数据")
	public ResResult chartRequest(String startDate,
								  @RequestParam(required = false) String endDate,
								  @PathVariable String bucketName,
								  @PathVariable ChartBy by,
								  @ApiIgnore @CurrentUser User currentUser
	) {
		Assert.notNull(startDate, "开始时间不能为空");
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "未拥有桶" + bucketName);
		return ResResult.success(auditService.findChartRequestList(bucket, startDate, endDate, by));
	}
}
