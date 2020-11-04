package mt.spring.mos.server.controller.member;

import mt.common.annotation.CurrentUser;
import mt.common.entity.ResResult;
import mt.spring.mos.sdk.MosSdk;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.dto.AccessControlAddDto;
import mt.spring.mos.server.entity.dto.AccessControlUpdateDto;
import mt.spring.mos.server.entity.dto.SignDto;
import mt.spring.mos.server.entity.po.AccessControl;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.entity.po.User;
import mt.spring.mos.server.service.AccessControlService;
import mt.spring.mos.server.service.BucketService;
import mt.spring.mos.server.service.ResourceService;
import mt.utils.Assert;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	
	@PostMapping("/{bucketName}")
	public ResResult add(@ApiIgnore @CurrentUser User currentUser, @PathVariable String bucketName, @RequestBody AccessControlAddDto accessControlAddDto) throws Exception {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "bucket不存在");
		accessControlAddDto.setBucketId(bucket.getId());
		return ResResult.success(accessControlService.addAccessControl(accessControlAddDto));
	}
	
	@PutMapping("/{bucketName}/{openId}")
	public ResResult update(@ApiIgnore @CurrentUser User currentUser, @PathVariable String bucketName, @PathVariable Long openId, @RequestBody AccessControlUpdateDto accessControlUpdateDto) {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "bucket不存在");
		accessControlUpdateDto.setBucketId(bucket.getId());
		accessControlUpdateDto.setOpenId(openId);
		return ResResult.success(accessControlService.updateAccessControl(accessControlUpdateDto));
	}
	
	@DeleteMapping("/{bucketName}/{openId}")
	public ResResult del(@ApiIgnore @CurrentUser User currentUser, @PathVariable String bucketName, @PathVariable Long openId) {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "bucket不存在");
		return ResResult.success(accessControlService.deleteAccessControl(bucket.getId(), openId));
	}
	
	@GetMapping("/{bucketName}")
	public ResResult list(@PathVariable String bucketName, @ApiIgnore @CurrentUser User currentUser) {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "bucket不存在");
		List<AccessControl> list = accessControlService.findList("bucketId", bucket.getId());
		if (CollectionUtils.isNotEmpty(list)) {
			for (AccessControl accessControl : list) {
				accessControl.setBucketName(bucketName);
			}
		}
		return ResResult.success(list);
	}
	
	@Autowired
	private ResourceService resourceService;
	@Autowired
	private MosServerProperties mosServerProperties;
	
	@PostMapping("/sign")
	public ResResult sign(@RequestBody SignDto signDto, @ApiIgnore @CurrentUser User currentUser) {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), signDto.getBucketName());
		Assert.notNull(bucket, "bucket不存在");
		AccessControl accessControl = accessControlService.findById(signDto.getOpenId());
		MosSdk mosSdk = new MosSdk(mosServerProperties.getDomain(), signDto.getOpenId(), bucket.getBucketName(), accessControl.getSecretKey());
		Resource resource = resourceService.findById(signDto.getResourceId());
		String signUrl;
		if (resource.getIsPublic()) {
			signUrl = getPublicUrl(resource.getPathname(), bucket.getBucketName(), mosSdk.getHost());
		} else {
			signUrl = mosSdk.getEncodedUrl(resource.getPathname(), signDto.getExpireSeconds());
		}
		return ResResult.success(signUrl);
	}
	
	private String getPublicUrl(@NotNull String pathname, String bucketName, String host) {
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		pathname = Stream.of(pathname.split("/")).map(s -> {
			try {
				return URLEncoder.encode(s, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}).collect(Collectors.joining("/"));
		return host +
				"/mos/" +
				bucketName +
				pathname;
	}
}
