package mt.spring.mos.server.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.sdk.MosSdk;
import mt.spring.mos.sdk.entity.MosConfig;
import mt.spring.mos.sdk.utils.MosEncrypt;
import mt.spring.mos.server.config.aop.SignChecker;
import mt.spring.mos.server.entity.BucketPerm;
import mt.spring.mos.server.entity.dto.AccessControlAddDto;
import mt.spring.mos.server.entity.dto.AccessControlUpdateDto;
import mt.spring.mos.server.entity.po.AccessControl;
import mt.spring.mos.server.entity.po.Bucket;
import mt.utils.common.BeanUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @Author Martin
 * @Date 2020/5/20
 */
@Service
@Slf4j
public class AccessControlService extends BaseServiceImpl<AccessControl> {
	@Autowired
	private final List<SignChecker> checkerList = new ArrayList<>();
	@Autowired
	private BucketGrantService bucketGrantService;
	public static final String ADMIN_SECRET_KEY = UUID.randomUUID().toString();
	
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
	
	public MosEncrypt.MosEncryptContent checkSign(HttpServletRequest request, String sign, Bucket bucket, BucketPerm[] perms, List<String> pathnames) {
		try {
			Assert.notNull(sign, "sign must not null");
			MosEncrypt.MosEncryptContent mosEncryptContent = MosEncrypt.decrypt(openId -> {
				if (openId <= 0) {
					return ADMIN_SECRET_KEY;
				}
				AccessControl accessControl = findById(openId);
				Assert.notNull(accessControl, "无效的openId");
				return accessControl.getSecretKey();
			}, sign);
			long expireSeconds = mosEncryptContent.getExpireSeconds();
			if (expireSeconds > 0) {
				long signTime = mosEncryptContent.getSignTime();
				if (System.currentTimeMillis() > signTime + expireSeconds * 1000) {
					//签名过期
					SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String signDate = simpleDateFormat.format(new Date(signTime));
					throw new IllegalStateException("签名已过期，签名时间：" + signDate + "，有效时间：" + expireSeconds + "秒");
				}
			}
			long openId = mosEncryptContent.getOpenId();
			if (openId > 0) {
				AccessControl accessControl = findById(openId);
				Long bucketId = accessControl.getBucketId();
				Assert.state(bucket.getId().equals(bucketId), "bucket校验错误");
				if (!bucketGrantService.hasPerms(accessControl, bucket, perms)) {
					throw new IllegalStateException("没有权限访问");
				}
			}
			for (SignChecker signChecker : checkerList) {
				if (signChecker.checkIsHasPerm(request, bucket, mosEncryptContent.getContent(), pathnames)) {
					return mosEncryptContent;
				}
			}
			throw new IllegalStateException("签名验证失败");
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new IllegalStateException("签名验证失败:" + e.getMessage(), e);
		}
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
	
	@Value("${server.port}")
	private Integer port;
	
	public interface MosSdkHandle<T> {
		T handle(@NotNull MosSdk mosSdk) throws Exception;
	}
	
	@SneakyThrows
	public <T> T useMosSdk(@NotNull Long openId, @NotNull String bucketName, @NotNull MosSdkHandle<T> handle) {
		MosSdk mosSdk = null;
		try {
			if (openId > 0) {
				AccessControl accessControl = findById(openId);
				MosConfig mosConfig = new MosConfig(List.of(""), bucketName, accessControl.getSecretKey(), openId);
				mosSdk = new MosSdk(mosConfig);
			} else {
				MosConfig mosConfig = new MosConfig(List.of("http://127.0.0.1:" + port), bucketName, ADMIN_SECRET_KEY, openId);
				mosSdk = new MosSdk(mosConfig);
			}
			return handle.handle(mosSdk);
		} finally {
			if (mosSdk != null) {
				mosSdk.shutdown();
			}
		}
	}
}
