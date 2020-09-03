package mt.spring.mos.server.controller.member;

import mt.spring.mos.server.service.BucketService;
import io.swagger.annotations.Api;
import mt.common.annotation.CurrentUser;
import mt.common.entity.ResResult;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.User;
import mt.utils.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Martin
 * @Date 2020/5/23
 */
@RestController
@RequestMapping("/member/bucket")
@Api(tags = "bucket管理")
public class BucketController {
	
	@Autowired
	private BucketService bucketService;
	
	@PostMapping
	public ResResult addBucket(String bucketName, @ApiIgnore @CurrentUser User currentUser) {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.state(bucket == null, "已存在此bucket");
		bucket = new Bucket();
		bucket.setBucketName(bucketName);
		bucket.setUserId(currentUser.getId());
		bucketService.save(bucket);
		return ResResult.success(bucket);
	}
	
	@DeleteMapping
	public ResResult delBucket(Long id, @ApiIgnore @CurrentUser User currentUser) {
		Bucket bucket = bucketService.findBucketByUserIdAndId(currentUser.getId(), id);
		Assert.notNull(bucket, "不存在此bucket");
		return ResResult.success(bucketService.deleteById(bucket));
	}
	
	@PutMapping
	public ResResult updateBucket(Long id, String bucketName, @ApiIgnore @CurrentUser User currentUser) {
		Bucket bucket = bucketService.findBucketByUserIdAndId(currentUser.getId(), id);
		Assert.notNull(bucket, "不存在此bucket");
		bucket.setBucketName(bucketName);
		bucketService.updateById(bucket);
		return ResResult.success(bucket);
	}
	
	@GetMapping("/list")
	public ResResult list(@ApiIgnore @CurrentUser User currentUser) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("userId", Filter.Operator.eq, currentUser.getId()));
		return ResResult.success(bucketService.findByFilters(filters));
	}
	
}
