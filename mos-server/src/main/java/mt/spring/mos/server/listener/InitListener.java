package mt.spring.mos.server.listener;

import lombok.extern.slf4j.Slf4j;
import mt.common.mybatis.event.AfterInitEvent;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.dto.AccessControlAddDto;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.User;
import mt.spring.mos.server.service.AccessControlService;
import mt.spring.mos.server.service.BucketService;
import mt.spring.mos.server.service.CacheService;
import mt.spring.mos.server.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * @Author Martin
 * @Date 2021/1/7
 */
@Component
@Slf4j
public class InitListener {
	@Autowired
	private MosServerProperties mosServerProperties;
	@Autowired
	private UserService userService;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private BucketService bucketService;
	@Autowired
	private AccessControlService accessControlService;
	@Autowired
	private CacheService cacheService;
	
	@EventListener
	public void init(AfterInitEvent afterInitEvent) {
		if (!afterInitEvent.isCreateDatabase()) {
			return;
		}
		
		if (StringUtils.isBlank(mosServerProperties.getAdminUsername())) {
			return;
		}
		
		cacheService.clearAll();
		
		User user = userService.findOne("username", mosServerProperties.getAdminUsername());
		if (user == null) {
			user = new User();
			user.setUsername(mosServerProperties.getAdminUsername());
			user.setPassword(passwordEncoder.encode(mosServerProperties.getAdminPassword()));
			user.setIsEnable(true);
			user.setIsAdmin(true);
			userService.save(user);
			
		}
		Bucket bucket = bucketService.findOne("bucketName", mosServerProperties.getDefaultBucketName());
		if (bucket == null) {
			bucket = new Bucket();
			bucket.setBucketName(mosServerProperties.getDefaultBucketName());
			bucket.setUserId(user.getId());
			bucketService.save(bucket);
			try {
				AccessControlAddDto accessControlAddDto = new AccessControlAddDto();
				accessControlAddDto.setBucketId(bucket.getId());
				accessControlAddDto.setUseInfo("默认");
				accessControlService.addAccessControl(user.getId(), accessControlAddDto);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}
}
