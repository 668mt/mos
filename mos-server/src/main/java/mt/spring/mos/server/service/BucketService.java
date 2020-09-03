package mt.spring.mos.server.service;

import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.dao.BucketMapper;
import mt.spring.mos.server.entity.po.Bucket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Martin
 * @Date 2020/5/23
 */
@Service
public class BucketService extends BaseServiceImpl<Bucket> {
	@Autowired
	private BucketMapper bucketMapper;
	
	@Override
	public BaseMapper<Bucket> getBaseMapper() {
		return bucketMapper;
	}
	
	public Bucket findBucketByUserIdAndId(Long userId, Long bucketId) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("id", Filter.Operator.eq, bucketId));
		filters.add(new Filter("userId", Filter.Operator.eq, userId));
		return findOneByFilters(filters);
	}
	
	public Bucket findBucketByUserIdAndBucketName(Long userId, String bucketName) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("bucketName", Filter.Operator.eq, bucketName));
		filters.add(new Filter("userId", Filter.Operator.eq, userId));
		return findOneByFilters(filters);
	}
	
	@Override
	@Cacheable("bucketCache")
	public Bucket findById(Object record) {
		return super.findById(record);
	}
}
