package mt.spring.mos.server.controller.member;

import io.swagger.v3.oas.annotations.tags.Tag;
import mt.common.annotation.CurrentUser;
import mt.common.entity.ResResult;
import mt.spring.mos.server.entity.dto.BucketAddDto;
import mt.spring.mos.server.entity.dto.BucketUpdateDto;
import mt.spring.mos.server.entity.po.User;
import mt.spring.mos.server.service.BucketService;
import mt.spring.mos.server.service.LockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.github.xiaoymin.knife4j.annotations.Ignore;

/**
 * @Author Martin
 * @Date 2020/5/23
 */
@RestController
@RequestMapping("/member/bucket")
@Tag(name = "bucket管理")
public class BucketController {
	
	@Autowired
	private BucketService bucketService;
	@Autowired
	private LockService lockService;
	
	@PostMapping
	public ResResult addBucket(@RequestBody BucketAddDto bucketAddDto, @Ignore @CurrentUser User currentUser) {
		String key = "addBucket";
		lockService.doWithLock(key, LockService.LockType.WRITE, () -> bucketService.addBucket(bucketAddDto, currentUser.getId()));
		return ResResult.success();
	}
	
	@DeleteMapping
	public ResResult delBucket(Long id, @Ignore @CurrentUser User currentUser) {
		return ResResult.success(bucketService.deleteBucket(id, currentUser.getId()));
	}
	
	@PutMapping
	public ResResult updateBucket(@RequestBody BucketUpdateDto bucketUpdateDto, @Ignore @CurrentUser User currentUser) {
		bucketService.updateBucket(bucketUpdateDto, currentUser.getId());
		return ResResult.success();
	}
	
	@GetMapping("/list")
	public ResResult list(@Ignore @CurrentUser User currentUser) {
		return ResResult.success(bucketService.findBucketList(currentUser.getId()));
	}
	
}
