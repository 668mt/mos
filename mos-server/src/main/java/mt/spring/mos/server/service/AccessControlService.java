package mt.spring.mos.server.service;

import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.sdk.utils.MosEncrypt;
import mt.spring.mos.server.dao.AccessControlMapper;
import mt.spring.mos.server.entity.dto.AccessControlAddDto;
import mt.spring.mos.server.entity.dto.AccessControlUpdateDto;
import mt.spring.mos.server.entity.po.AccessControl;
import mt.spring.mos.server.utils.MosSignUtils;
import mt.utils.common.BeanUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Martin
 * @Date 2020/5/20
 */
@Service
public class AccessControlService extends BaseServiceImpl<AccessControl> {
	@Autowired
	private AccessControlMapper accessControlMapper;
	
	@Override
	public BaseMapper<AccessControl> getBaseMapper() {
		return accessControlMapper;
	}
	
	/**
	 * 生成公钥、私钥
	 */
	@CacheEvict(value = "accessControlCache", allEntries = true)
	public AccessControl addAccessControl(Long userId, AccessControlAddDto accessControlAddDto) throws Exception {
		AccessControl accessControl = new AccessControl();
		accessControl.setUserId(userId);
		accessControl.setUseInfo(accessControlAddDto.getUseInfo());
		accessControl.setBucketId(accessControlAddDto.getBucketId());
		accessControl.setSecretKey(MosEncrypt.generateKey());
		save(accessControl);
		return accessControl;
	}
	
	public MosEncrypt.MosEncryptContent checkSign(@NotNull("sign不能为空") String sign, @NotNull("pathname不能为空") String pathname, @NotNull("bucketName不能为空") String bucketName) {
		Assert.notNull(sign, "sign不能为空");
		Assert.notNull(pathname, "pathname不能为空");
		Assert.notNull(bucketName, "bucketName不能为空");
		return MosSignUtils.checkSign(pathname, sign, openId -> {
			AccessControl accessControl = findById(openId);
			Assert.notNull(accessControl, "无效的openId");
			return accessControl.getSecretKey();
		}, bucketName);
	}
	
	@Override
	@Cacheable("accessControlCache")
	public AccessControl findById(Object record) {
		return super.findById(record);
	}
	
	@Transactional
	@CacheEvict(value = "accessControlCache", allEntries = true)
	public int deleteAccessControl(Long userId, Long bucketId, Long openId) {
		AccessControl accessControl = findById(openId);
		Assert.state(accessControl != null && accessControl.getUserId().equals(userId), "不能越权删除");
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("bucketId", Filter.Operator.eq, bucketId));
		filters.add(new Filter("openId", Filter.Operator.eq, openId));
		filters.add(new Filter("userId", Filter.Operator.eq, userId));
		return deleteByFilters(filters);
	}
	
	@Transactional
	@CacheEvict(value = "accessControlCache", allEntries = true)
	public int updateAccessControl(Long userId, AccessControlUpdateDto accessControlUpdateDto) {
		Long openId = accessControlUpdateDto.getOpenId();
		AccessControl findAccessControl = findById(openId);
		Assert.state(findAccessControl != null && findAccessControl.getUserId().equals(userId), "不能越权修改");
		AccessControl accessControl = BeanUtils.transformOf(accessControlUpdateDto, AccessControl.class);
		return updateByIdSelective(accessControl);
	}
	
	@Cacheable("accessControlCache")
	public List<AccessControl> findOwnList(Long userId, Long bucketId) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("userId", Filter.Operator.eq, userId));
		filters.add(new Filter("bucketId", Filter.Operator.eq, bucketId));
		return findByFilters(filters);
	}
}
