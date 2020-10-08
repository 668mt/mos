package mt.spring.mos.server.service;

import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.dao.BucketMapper;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.BucketGrant;
import mt.spring.mos.server.entity.vo.BucketVo;
import mt.utils.Assert;
import mt.utils.MyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author Martin
 * @Date 2020/5/23
 */
@Service
public class BucketService extends BaseServiceImpl<Bucket> {
	@Autowired
	private BucketMapper bucketMapper;
	@Autowired
	@Lazy
	private BucketGrantService bucketGrantService;
	@Autowired
	@Lazy
	private AccessControlService accessControlService;
	@Autowired
	@Lazy
	private ResourceService resourceService;
	
	@Override
	public BaseMapper<Bucket> getBaseMapper() {
		return bucketMapper;
	}
	
	public Bucket findBucketByUserIdAndId(Long userId, Long bucketId) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("id", Filter.Operator.eq, bucketId));
		filters.add(new Filter("userId", Filter.Operator.eq, userId));
		Bucket bucket = findOneByFilters(filters);
		if (bucket == null) {
			bucket = bucketMapper.findGrantBucketByUserIdAndBucketId(userId, bucketId);
		}
		return bucket;
	}
	
	public Bucket findBucketByUserIdAndBucketName(Long userId, String bucketName) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("bucketName", Filter.Operator.eq, bucketName));
		filters.add(new Filter("userId", Filter.Operator.eq, userId));
		Bucket bucket = findOneByFilters(filters);
		if (bucket == null) {
			bucket = bucketMapper.findGrantBucketByUserIdAndBucketName(userId, bucketName);
		}
		return bucket;
	}
	
	@Override
	@Cacheable("bucketCache")
	public Bucket findById(Object record) {
		return super.findById(record);
	}
	
	public List<BucketVo> findBucketList(Long userId) {
		return bucketMapper.findBucketList(userId);
	}
	
	@CacheEvict(value = "bucketCache", allEntries = true)
	@Transactional
	public int deleteBucket(Long bucketId, Long userId) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("id", Filter.Operator.eq, bucketId));
		filters.add(new Filter("userId", Filter.Operator.eq, userId));
		Bucket bucket = findOneByFilters(filters);
		Assert.notNull(bucket, "不能删除不属于自己的bucket");
		//判断是否有被授权
		List<BucketGrant> grantList = bucketGrantService.findList("bucketId", bucketId);
		Assert.state(MyUtils.isEmpty(grantList), "该bucket已授权给用户，请先取消对应的授权");
		//删除openId
		accessControlService.deleteByFilters(Collections.singletonList(new Filter("bucketId", Filter.Operator.eq, bucketId)));
		//删除资源
		resourceService.deleteAllResources(bucketId);
		return deleteById(bucket);
	}
}
