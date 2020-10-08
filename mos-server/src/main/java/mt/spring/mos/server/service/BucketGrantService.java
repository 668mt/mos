package mt.spring.mos.server.service;

import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.dao.BucketGrantMapper;
import mt.spring.mos.server.entity.po.BucketGrant;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * @Author Martin
 * @Date 2020/10/8
 */
@Service
public class BucketGrantService extends BaseServiceImpl<BucketGrant> {
	@Autowired
	private BucketGrantMapper bucketGrantMapper;
//	@Autowired
//	@Lazy
//	private BucketService bucketService;
	
	@Override
	public BaseMapper<BucketGrant> getBaseMapper() {
		return bucketGrantMapper;
	}
	
	@Transactional
	public void grant(@NotNull Long bucketId, @NotNull List<Long> userIds) {
		deleteByFilters(Collections.singletonList(new Filter("bucketId", Filter.Operator.eq, bucketId)));
		for (Long userId : userIds) {
			BucketGrant bucketGrant = new BucketGrant();
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
}
