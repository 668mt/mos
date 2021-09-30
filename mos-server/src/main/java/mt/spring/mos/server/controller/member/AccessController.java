package mt.spring.mos.server.controller.member;

import mt.common.annotation.CurrentUser;
import mt.common.entity.ResResult;
import mt.spring.mos.sdk.MosSdk;
import mt.spring.mos.server.annotation.NeedPerm;
import mt.spring.mos.server.entity.BucketPerm;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.dto.AccessControlAddDto;
import mt.spring.mos.server.entity.dto.AccessControlUpdateDto;
import mt.spring.mos.server.entity.dto.SignDto;
import mt.spring.mos.server.entity.po.*;
import mt.spring.mos.server.service.AccessControlService;
import mt.spring.mos.server.service.BucketService;
import mt.spring.mos.server.service.DirService;
import mt.spring.mos.server.service.ResourceService;
import mt.utils.common.Assert;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
	@Autowired
	private ResourceService resourceService;
	@Autowired
	private MosServerProperties mosServerProperties;
	@Autowired
	private DirService dirService;
	
	@PostMapping("/{bucketName}")
	@NeedPerm(BucketPerm.INSERT)
	public ResResult add(@ApiIgnore @CurrentUser User currentUser, @PathVariable String bucketName, @RequestBody AccessControlAddDto accessControlAddDto) throws Exception {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "bucket不存在");
		accessControlAddDto.setBucketId(bucket.getId());
		return ResResult.success(accessControlService.addAccessControl(currentUser.getId(), accessControlAddDto));
	}
	
	@PutMapping("/{bucketName}/{openId}")
	@NeedPerm(BucketPerm.UPDATE)
	public ResResult update(@ApiIgnore @CurrentUser User currentUser, @PathVariable String bucketName, @PathVariable Long openId, @RequestBody AccessControlUpdateDto accessControlUpdateDto) {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "bucket不存在");
		accessControlUpdateDto.setBucketId(bucket.getId());
		accessControlUpdateDto.setOpenId(openId);
		return ResResult.success(accessControlService.updateAccessControl(currentUser.getId(), accessControlUpdateDto));
	}
	
	@DeleteMapping("/{bucketName}/{openId}")
	@NeedPerm(BucketPerm.DELETE)
	public ResResult del(@ApiIgnore @CurrentUser User currentUser, @PathVariable String bucketName, @PathVariable Long openId) {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "bucket不存在");
		return ResResult.success(accessControlService.deleteAccessControl(currentUser.getId(), bucket.getId(), openId));
	}
	
	@GetMapping("/{bucketName}")
	@NeedPerm(BucketPerm.SELECT)
	public ResResult list(@PathVariable String bucketName, @ApiIgnore @CurrentUser User currentUser) {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "bucket不存在");
		List<AccessControl> list = accessControlService.findOwnList(currentUser.getId(), bucket.getId());
		if (CollectionUtils.isNotEmpty(list)) {
			for (AccessControl accessControl : list) {
				accessControl.setBucketName(bucketName);
			}
		}
		return ResResult.success(list);
	}
	
	@PostMapping("/sign")
	@NeedPerm(BucketPerm.SELECT)
	public ResResult sign(@RequestBody SignDto signDto, @ApiIgnore @CurrentUser User currentUser, HttpServletRequest request) {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), signDto.getBucketName());
		Assert.notNull(bucket, "bucket不存在");
		AccessControl accessControl = accessControlService.findById(signDto.getOpenId());
		Assert.state(accessControl.getUserId().equals(currentUser.getId()), "openId无效");
		MosSdk mosSdk = new MosSdk(mosServerProperties.getDomain(), signDto.getOpenId(), bucket.getBucketName(), accessControl.getSecretKey());
		Long resourceId = signDto.getResourceId();
		String signUrl;
		Resource resource = null;
		if (resourceId != null) {
			resource = resourceService.findResourceByIdAndBucketId(resourceId, bucket.getId());
			Assert.notNull(resource, "资源不存在");
			String pathname = resourceService.getPathname(resource);
			signUrl = mosSdk.getUrl(pathname, signDto.getExpireSeconds(), TimeUnit.SECONDS, mosSdk.getMosConfig().getHost(), signDto.getRender(), false);
		} else {
			Assert.notNull(signDto.getDirId(), "未传入resourceId或dirId");
			Dir dir = dirService.findOneByDirIdAndBucketId(signDto.getDirId(), bucket.getId(), false);
			Assert.notNull(dir, "路径不存在");
			signUrl = mosSdk.getUrl(dir.getPath(), signDto.getExpireSeconds(), TimeUnit.SECONDS, mosSdk.getMosConfig().getHost(), signDto.getRender(), true);
		}
		
		if (resource != null && resource.getIsPublic()) {
			int lastIndexOf = signUrl.lastIndexOf("?");
			if (lastIndexOf != -1) {
				signUrl = signUrl.substring(0, lastIndexOf);
			}
		}
		return ResResult.success(signUrl);
	}
}
