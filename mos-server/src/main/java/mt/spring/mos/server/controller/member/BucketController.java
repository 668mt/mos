package mt.spring.mos.server.controller.member;

import io.swagger.annotations.Api;
import mt.common.annotation.CurrentUser;
import mt.common.entity.ResResult;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.User;
import mt.spring.mos.server.service.BucketService;
import mt.utils.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

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
		bucketService.addBucket(bucketName, currentUser.getId());
		return ResResult.success();
	}
	
	@DeleteMapping
	public ResResult delBucket(Long id, @ApiIgnore @CurrentUser User currentUser) {
		return ResResult.success(bucketService.deleteBucket(id, currentUser.getId()));
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
		return ResResult.success(bucketService.findBucketList(currentUser.getId()));
	}
	
}
