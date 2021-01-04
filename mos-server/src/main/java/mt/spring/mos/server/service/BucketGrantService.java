package mt.spring.mos.server.service;

import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.dao.BucketGrantMapper;
import mt.spring.mos.server.entity.BucketPerm;
import mt.spring.mos.server.entity.dto.BucketGrantDto;
import mt.spring.mos.server.entity.po.AccessControl;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.BucketGrant;
import mt.spring.mos.server.entity.vo.BucketPermVo;
import mt.spring.mos.server.entity.vo.BucketVo;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2020/10/8
 */
@Service
public class BucketGrantService extends BaseServiceImpl<BucketGrant> {
	@Autowired
	private BucketGrantMapper bucketGrantMapper;
	public static final List<BucketPerm> ALL_PERMS = Arrays.asList(BucketPerm.values());
	@Autowired
	@Lazy
	private BucketService bucketService;
	
	@Override
	public BaseMapper<BucketGrant> getBaseMapper() {
		return bucketGrantMapper;
	}
	
	@Transactional
	@CacheEvict(value = "permCache", allEntries = true)
	public void grant(BucketGrantDto bucketGrantDto) {
		Long bucketId = bucketGrantDto.getBucketId();
		deleteByFilters(Collections.singletonList(new Filter("bucketId", Filter.Operator.eq, bucketId)));
		List<BucketGrantDto.GrantBody> grants = bucketGrantDto.getGrants();
		for (BucketGrantDto.GrantBody grant : grants) {
			Long userId = grant.getUserId();
			List<BucketPerm> perms = grant.getPerms();
			BucketGrant bucketGrant = new BucketGrant();
			bucketGrant.setPerms(perms);
			bucketGrant.setBucketId(bucketId);
			bucketGrant.setUserId(userId);
			save(bucketGrant);
		}
	}
	
	public BucketGrant findById(Long bucketId, Long userId) {
		BucketGrant bucketGrant = new BucketGrant();
		bucketGrant.setBucketId(bucketId);
		bucketGrant.setUserId(userId);
		return findById(bucketGrant);
	}
	
	public boolean hasPerms(@NotNull AccessControl accessControl, @NotNull Bucket bucket, @NotNull BucketPerm... perms) {
		Long userId = accessControl.getUserId();
		return hasPerms(userId, bucket, perms);
	}
	
	@Cacheable(value = "permCache")
	public boolean hasPerms(@NotNull Long userId, @NotNull Bucket bucket, @NotNull BucketPerm... perms) {
		boolean hasPerm = false;
		Long ownUserId = bucket.getUserId();
		if (userId.equals(ownUserId)) {
			hasPerm = true;
		} else {
			BucketGrant bucketGrant = findById(bucket.getId(), userId);
			if (bucketGrant != null) {
				List<BucketPerm> bucketPerms = bucketGrant.getPerms();
				if (bucketPerms == null) {
					bucketPerms = new ArrayList<>();
				}
				boolean match = true;
				for (BucketPerm perm : perms) {
					if (!bucketPerms.contains(perm)) {
						match = false;
						break;
					}
				}
				hasPerm = match;
			}
		}
		return hasPerm;
	}
	
	@Cacheable(value = "permCache")
	public List<BucketPermVo> findOwnPerms(Long userId) {
		List<BucketVo> bucketList = bucketService.findBucketList(userId);
		if (bucketList == null) {
			return new ArrayList<>();
		}
		return bucketList.stream().map(bucketVo -> {
			Boolean isOwn = bucketVo.getIsOwn();
			BucketPermVo bucketPermVo = new BucketPermVo();
			bucketPermVo.setBucketName(bucketVo.getBucketName());
			List<BucketPerm> perms;
			if (isOwn) {
				perms = ALL_PERMS;
			} else {
				BucketGrant bucketGrant = findById(bucketVo.getId(), userId);
				perms = bucketGrant.getPerms();
			}
			bucketPermVo.setPerms(perms);
			return bucketPermVo;
		}).collect(Collectors.toList());
	}
}
