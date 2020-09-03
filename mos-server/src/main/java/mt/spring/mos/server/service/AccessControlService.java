package mt.spring.mos.server.service;

import mt.spring.mos.sdk.RSAUtils;
import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.spring.mos.server.dao.AccessControlMapper;
import mt.spring.mos.server.entity.po.AccessControl;
import mt.spring.mos.server.utils.MosSignUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;

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
	 * @param bucketId
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public AccessControl generate(@NotNull Long bucketId) throws NoSuchAlgorithmException {
		AccessControl accessControl = findOne("bucketId", bucketId);
		String[] keys = RSAUtils.genKeyPair();
		if (accessControl == null) {
			accessControl = new AccessControl();
			accessControl.setBucketId(bucketId);
			accessControl.setPublicKey(keys[0]);
			accessControl.setPrivateKey(keys[1]);
			save(accessControl);
		} else {
			accessControl.setPublicKey(keys[0]);
			accessControl.setPrivateKey(keys[1]);
			updateById(accessControl);
		}
		return accessControl;
	}
	
	public void checkSign(@NotNull("openId不能为空") Long openId, @NotNull("sign不能为空") String sign, @NotNull("pathname不能为空") String pathname, @NotNull("bucketName不能为空") String bucketName) {
		Assert.notNull(openId, "openId不能为空");
		Assert.notNull(sign, "sign不能为空");
		Assert.notNull(pathname, "pathname不能为空");
		Assert.notNull(bucketName, "bucketName不能为空");
		AccessControl accessControl = findById(openId);
		Assert.notNull(accessControl, "无效的openId");
		try {
			pathname = URLDecoder.decode(pathname, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		MosSignUtils.checkSign(pathname, sign, accessControl.getPrivateKey(), bucketName);
	}
}
