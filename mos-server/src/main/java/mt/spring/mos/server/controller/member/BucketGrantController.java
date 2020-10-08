package mt.spring.mos.server.controller.member;

import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import mt.common.annotation.CurrentUser;
import mt.common.entity.ResResult;
import mt.common.utils.BeanUtils;
import mt.spring.mos.server.entity.dto.BucketGrantCondition;
import mt.spring.mos.server.entity.dto.BucketGrantDto;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.BucketGrant;
import mt.spring.mos.server.entity.po.User;
import mt.spring.mos.server.entity.vo.UserGrantVo;
import mt.spring.mos.server.service.BucketGrantService;
import mt.spring.mos.server.service.BucketService;
import mt.spring.mos.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2020/10/8
 */
@RestController
@RequestMapping("/member/bucket/grant")
@Api(tags = "Bucket授权")
public class BucketGrantController {
	@Autowired
	private BucketGrantService bucketGrantService;
	@Autowired
	private UserService userService;
	@Autowired
	private BucketService bucketService;
	
	@GetMapping("/list")
	@ApiOperation("查询授权列表")
	public ResResult list(Long bucketId, @ApiIgnore @CurrentUser User currentUser) {
		BucketGrantCondition bucketGrantCondition = new BucketGrantCondition();
		bucketGrantCondition.setBucketId(bucketId);
		PageInfo<BucketGrant> page = bucketGrantService.findPage(0, 0, null, bucketGrantCondition);
		List<BucketGrant> grantList = page.getList();
		List<User> users = userService.findAll();
		Bucket bucket = bucketService.findById(bucketId);
		
		List<UserGrantVo> allUsers = users.stream().filter(user -> !user.getId().equals(bucket.getUserId()))
				.filter(user -> !user.getId().equals(currentUser.getId())).map(user -> {
					UserGrantVo transform = BeanUtils.transform(UserGrantVo.class, user);
					transform.setKey(transform.getId() + "");
					transform.setTitle(transform.getUsername());
					return transform;
				}).collect(Collectors.toList());
		List<String> userKeys = grantList.stream().map(bucketGrant -> bucketGrant.getUserId() + "").collect(Collectors.toList());
		Map<String, Object> data = new HashMap<>();
		data.put("allUsers", allUsers);
		data.put("userKeys", userKeys);
		return ResResult.success(data);
	}
	
	@PostMapping
	@ApiOperation("授权")
	public ResResult grant(@RequestBody BucketGrantDto bucketGrantDto) {
		bucketGrantService.grant(bucketGrantDto.getBucketId(), bucketGrantDto.getUserIds());
		return ResResult.success();
	}
}
