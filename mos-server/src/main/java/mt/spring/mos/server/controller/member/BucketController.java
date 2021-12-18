package mt.spring.mos.server.controller.member;

import io.swagger.annotations.Api;
import mt.common.annotation.CurrentUser;
import mt.common.entity.ResResult;
import mt.spring.mos.server.entity.dto.BucketAddDto;
import mt.spring.mos.server.entity.dto.BucketUpdateDto;
import mt.spring.mos.server.entity.po.User;
import mt.spring.mos.server.service.BucketService;
import mt.spring.mos.server.service.LockService;
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
	@Autowired
	private LockService lockService;
	
	@PostMapping
	public ResResult addBucket(@RequestBody BucketAddDto bucketAddDto, @ApiIgnore @CurrentUser User currentUser) {
		String key = "addBucket";
		lockService.doWithLock(key, LockService.LockType.WRITE, 2, () -> {
			bucketService.addBucket(bucketAddDto, currentUser.getId());
			return null;
		});
		return ResResult.success();
	}
	
	@DeleteMapping
	public ResResult delBucket(Long id, @ApiIgnore @CurrentUser User currentUser) {
		return ResResult.success(bucketService.deleteBucket(id, currentUser.getId()));
	}
	
	@PutMapping
	public ResResult updateBucket(@RequestBody BucketUpdateDto bucketUpdateDto, @ApiIgnore @CurrentUser User currentUser) {
		bucketService.updateBucket(bucketUpdateDto, currentUser.getId());
		return ResResult.success();
	}
	
	@GetMapping("/list")
	public ResResult list(@ApiIgnore @CurrentUser User currentUser) {
		return ResResult.success(bucketService.findBucketList(currentUser.getId()));
	}
	
}
