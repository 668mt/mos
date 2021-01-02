package mt.spring.mos.server.controller.member;

import mt.common.annotation.CurrentUser;
import mt.common.entity.ResResult;
import mt.spring.mos.sdk.utils.Assert;
import mt.spring.mos.server.annotation.NeedPerm;
import mt.spring.mos.server.entity.BucketPerm;
import mt.spring.mos.server.entity.dto.DirAddDto;
import mt.spring.mos.server.entity.dto.DirUpdateDto;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Dir;
import mt.spring.mos.server.entity.po.User;
import mt.spring.mos.server.service.BucketService;
import mt.spring.mos.server.service.DirService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @Author Martin
 * @Date 2020/12/5
 */
@RestController
@RequestMapping("/member/dir")
public class DirController {
	
	@Autowired
	private DirService dirService;
	@Autowired
	private BucketService bucketService;
	
	@GetMapping("/{bucketName}")
	@NeedPerm(BucketPerm.SELECT)
	public ResResult findByPath(@ApiIgnore @CurrentUser User currentUser, @PathVariable String bucketName, String path) {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "不存在bucket：" + bucketName);
		Dir dir = dirService.findOneByPathAndBucketId(path, bucket.getId());
		return ResResult.success(dir);
	}
	
	@PutMapping("/{bucketName}/{id}")
	@NeedPerm(BucketPerm.UPDATE)
	public ResResult update(@ApiIgnore @CurrentUser User currentUser, @PathVariable String bucketName, @PathVariable Long id, @RequestBody DirUpdateDto dirUpdateDto) {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "不存在bucket：" + bucketName);
		dirUpdateDto.setBucketName(bucketName);
		dirUpdateDto.setId(id);
		dirService.updatePath(bucket.getId(), dirUpdateDto);
		return ResResult.success();
	}
	
	@PutMapping("/{bucketName}/merge/{srcId}/to/{desId}")
	@NeedPerm(BucketPerm.UPDATE)
	public ResResult merge(@ApiIgnore @CurrentUser User currentUser, @PathVariable String bucketName, @PathVariable Long srcId, @PathVariable Long desId) {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "不存在bucket：" + bucketName);
		dirService.mergeDir(bucket.getId(), srcId, desId);
		return ResResult.success();
	}
	
	
	@PostMapping("/{bucketName}")
	@NeedPerm(BucketPerm.INSERT)
	public ResResult add(@ApiIgnore @CurrentUser User currentUser, @PathVariable String bucketName, @RequestBody DirAddDto dirAddDto) {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "不存在bucket：" + bucketName);
		dirService.addDir(dirAddDto.getPath(), bucket.getId());
		return ResResult.success();
	}
}
