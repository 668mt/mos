package mt.spring.mos.server.service;

import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.sdk.RSAUtils;
import mt.spring.mos.server.dao.AccessControlMapper;
import mt.spring.mos.server.entity.dto.AccessControlAddDto;
import mt.spring.mos.server.entity.dto.AccessControlUpdateDto;
import mt.spring.mos.server.entity.po.AccessControl;
import mt.spring.mos.server.utils.MosSignUtils;
import mt.utils.BeanUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.security.NoSuchAlgorithmException;
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
	 *
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public AccessControl addAccessControl(AccessControlAddDto accessControlAddDto) throws NoSuchAlgorithmException {
		String[] keys = RSAUtils.genKeyPair();
		AccessControl accessControl = new AccessControl();
		accessControl.setUseInfo(accessControlAddDto.getUseInfo());
		accessControl.setBucketId(accessControlAddDto.getBucketId());
		accessControl.setPublicKey(keys[0]);
		accessControl.setPrivateKey(keys[1]);
		save(accessControl);
		return accessControl;
	}
	
	public void checkSign(@NotNull("openId不能为空") Long openId, @NotNull("sign不能为空") String sign, @NotNull("pathname不能为空") String pathname, @NotNull("bucketName不能为空") String bucketName) {
		Assert.notNull(openId, "openId不能为空");
		Assert.notNull(sign, "sign不能为空");
		Assert.notNull(pathname, "pathname不能为空");
		Assert.notNull(bucketName, "bucketName不能为空");
		AccessControl accessControl = findById(openId);
		Assert.notNull(accessControl, "无效的openId");
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		MosSignUtils.checkSign(pathname, sign, accessControl.getPrivateKey(), bucketName);
	}
	
	@Transactional
	public int deleteAccessControl(Long bucketId, Long openId) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("bucketId", Filter.Operator.eq, bucketId));
		filters.add(new Filter("openId", Filter.Operator.eq, openId));
		return deleteByFilters(filters);
	}
	
	@Transactional
	public int updateAccessControl(AccessControlUpdateDto accessControlUpdateDto) {
		AccessControl accessControl = BeanUtils.transformOf(accessControlUpdateDto, AccessControl.class);
		return updateByIdSelective(accessControl);
	}
}
