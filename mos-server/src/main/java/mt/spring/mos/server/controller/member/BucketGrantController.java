package mt.spring.mos.server.controller.member;

import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import mt.common.annotation.CurrentUser;
import mt.common.entity.ResResult;
import mt.common.utils.BeanUtils;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.server.entity.BucketPerm;
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
import com.github.xiaoymin.knife4j.annotations.Ignore;

import java.util.ArrayList;
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
@Tag(name = "Bucket授权")
public class BucketGrantController {
    @Autowired
    private BucketGrantService bucketGrantService;
    @Autowired
    private UserService userService;
    @Autowired
    private BucketService bucketService;

    private List<BucketPerm> findPerms(List<BucketGrant> grants, Long userId) {
        BucketGrant findBucketGrant = grants.stream().filter(bucketGrant -> bucketGrant.getUserId().equals(userId)).findFirst().orElse(null);
        if (findBucketGrant != null) {
            return findBucketGrant.getPerms();
        }
        return null;
    }

    @GetMapping("/list")
    @Operation(summary = "查询授权列表")
    public ResResult list(Long bucketId, @Ignore @CurrentUser User currentUser) {
        Bucket bucket = bucketService.findById(bucketId);
        mustBeOwner(currentUser, bucket);
        BucketGrantCondition bucketGrantCondition = new BucketGrantCondition();
        bucketGrantCondition.setBucketId(bucketId);
        PageInfo<BucketGrant> page = bucketGrantService.findPage(0, 0, null, bucketGrantCondition);
        List<BucketGrant> grantList = page.getList();
        List<User> users = userService.findAll();

        List<UserGrantVo> allUsers = users.stream()
                //排除当前用户
                .filter(user -> !user.getId().equals(bucket.getUserId()))
                .filter(user -> !user.getId().equals(currentUser.getId())).map(user -> {
                    UserGrantVo transform = BeanUtils.transform(UserGrantVo.class, user);
                    transform.setKey(transform.getId() + "");
                    transform.setTitle(transform.getUsername());
                    transform.setPerms(findPerms(grantList, user.getId()));
                    return transform;
                }).collect(Collectors.toList());
        List<String> userKeys = grantList.stream().map(bucketGrant -> bucketGrant.getUserId() + "").collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("allUsers", allUsers);
        data.put("userKeys", userKeys);
        return ResResult.success(data);
    }

    @PostMapping
    @Operation(summary = "授权")
    public ResResult grant(@RequestBody BucketGrantDto bucketGrantDto, @CurrentUser User currentUser) {
        Bucket bucket = bucketService.findBucketByUserIdAndId(currentUser.getId(), bucketGrantDto.getBucketId());
        mustBeOwner(currentUser, bucket);
        bucketGrantService.grant(bucketGrantDto);
        return ResResult.success();
    }

    @GetMapping("/perms/all")
    @Operation(summary = "获取所有的权限列表")
    public ResResult perms() {
        return ResResult.success(BucketPerm.values());
    }

    private void mustBeOwner(User currentUser, Bucket bucket) {
        Assert.state(bucket != null && bucket.getUserId().equals(currentUser.getId()), "没有权限");
    }

    @GetMapping("/perms/own")
    @Operation(summary = "获取拥有的权限")
    public ResResult ownPerms(@CurrentUser User currentUser) {
        if (currentUser == null) {
            return ResResult.success(new ArrayList<>());
        }
        return ResResult.success(bucketGrantService.findOwnPerms(currentUser.getId()));
    }
}
