package mt.spring.mos.server.controller.member;

import mt.spring.mos.server.service.AccessControlService;
import mt.spring.mos.server.service.BucketService;
import mt.common.annotation.CurrentUser;
import mt.common.entity.ResResult;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.User;
import mt.utils.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author Martin
 * @Date 2020/5/28
 */
@RestController
@RequestMapping("/member/access")
public class AccessController {
	@Autowired
	private AccessControlService accessControlService;
	@Autowired
	private BucketService bucketService;
	
	@PostMapping("/{bucketId}")
	public ResResult generate(@ApiIgnore @CurrentUser User currentUser, @PathVariable Long bucketId) throws NoSuchAlgorithmException {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("id", Filter.Operator.eq, bucketId));
		filters.add(new Filter("userId", Filter.Operator.eq, currentUser.getId()));
		Bucket bucket = bucketService.findOneByFilters(filters);
		Assert.notNull(bucket, "bucket不存在");
		return ResResult.success(accessControlService.generate(bucketId));
	}
	
	@GetMapping("/{bucketId}")
	public ResResult list(@PathVariable Long bucketId, @ApiIgnore @CurrentUser User currentUser) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("id", Filter.Operator.eq, bucketId));
		filters.add(new Filter("userId", Filter.Operator.eq, currentUser.getId()));
		Bucket bucket = bucketService.findOneByFilters(filters);
		Assert.notNull(bucket, "不存在此bucket");
		return ResResult.success(accessControlService.findList("bucketId", bucket.getId()));
	}
}
