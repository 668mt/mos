package mt.spring.mos.server.service;

import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.common.utils.BeanUtils;
import mt.spring.mos.server.dao.BucketMapper;
import mt.spring.mos.server.entity.dto.BucketAddDto;
import mt.spring.mos.server.entity.dto.BucketUpdateDto;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.BucketGrant;
import mt.spring.mos.server.entity.po.Dir;
import mt.spring.mos.server.entity.vo.BucketVo;
import mt.utils.Assert;
import mt.utils.MyUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
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
	@Autowired
	@Lazy
	private DirService dirService;
	
	@Override
	public BaseMapper<Bucket> getBaseMapper() {
		return bucketMapper;
	}
	
	@Cacheable("bucketCache")
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
	
	@Cacheable(value = "bucketCache", unless = "#result == null ")
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
	public Bucket findOne(String column, Object value) {
		return super.findOne(column, value);
	}
	
	@Override
	@Cacheable("bucketCache")
	public Bucket findById(Object record) {
		return super.findById(record);
	}
	
	@Cacheable("bucketCache")
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
		//资源必须为空
		List<Dir> dirs = dirService.findList("bucketId", bucketId);
		if (dirs != null) {
			Assert.state(dirs.size() <= 1, "资源不为空，不能进行删除");
			if (dirs.size() == 1) {
				Dir dir = dirs.get(0);
				Assert.state("/".equals(dir.getPath()), "资源不为空，不能进行删除");
				Assert.state(!resourceService.exists("dirId", dir.getId()), "资源不为空，不能进行删除");
				dirService.deleteById(dir);
			}
		}
		//判断是否有被授权
		List<BucketGrant> grantList = bucketGrantService.findList("bucketId", bucketId);
		Assert.state(MyUtils.isEmpty(grantList), "该bucket已授权给用户，请先取消对应的授权");
		//删除openId
		accessControlService.deleteByFilters(Collections.singletonList(new Filter("bucketId", Filter.Operator.eq, bucketId)));
		return deleteById(bucket);
	}
	
	private void checkBucketName(String bucketName, @Nullable Long bucketId) {
		Assert.state(bucketName.length() >= 2, "bucket名称的长度至少为2");
		Assert.state(bucketName.length() <= 20, "bucket名称的长度最大为20");
		Assert.state(bucketName.matches("^\\w*[a-zA-Z]\\w*$"), "bucket名称不符合规则，请输入数字和字母的组合，至少包含一位字母");
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("bucketName", Filter.Operator.eq, bucketName));
		if (bucketId != null) {
			filters.add(new Filter("id", Filter.Operator.ne, bucketId));
		}
		Bucket bucket = findOneByFilters(filters);
		Assert.state(bucket == null, "bucket名称已重复，请换个名字");
	}
	
	@Transactional
	@CacheEvict(value = "bucketCache", allEntries = true)
	public void addBucket(BucketAddDto bucketAddDto, Long userId) {
		String bucketName = bucketAddDto.getBucketName();
		checkBucketName(bucketName, null);
		Bucket bucket = BeanUtils.transform(Bucket.class, bucketAddDto);
		bucket.setUserId(userId);
		save(bucket);
	}
	
	@Transactional
	@CacheEvict(value = "bucketCache", allEntries = true)
	public void updateBucket(BucketUpdateDto bucketUpdateDto, Long userId) {
		Bucket bucket = findBucketByUserIdAndId(userId, bucketUpdateDto.getId());
		Assert.notNull(bucket, "不存在此bucket");
		bucket = BeanUtils.transform(Bucket.class, bucketUpdateDto);
		if (StringUtils.isNotBlank(bucket.getBucketName())) {
			checkBucketName(bucket.getBucketName(), bucket.getId());
		}
		updateByIdSelective(bucket);
	}
}
