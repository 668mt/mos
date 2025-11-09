package mt.spring.mos.server.entity.handler;

import jakarta.servlet.http.HttpServletRequest;
import mt.common.starter.message.messagehandler.MessageHandler;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.base.utils.CollectionUtils;
import mt.spring.mos.sdk.MosSdk;
import mt.spring.mos.sdk.entity.MosConfig;
import mt.spring.mos.sdk.entity.params.UrlBuildParams;
import mt.spring.mos.server.config.aop.MosContext;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.po.AccessControl;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.service.AccessControlService;
import mt.spring.mos.server.service.BucketService;
import mt.spring.mos.server.utils.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2021/8/21
 */
@Component
public class SignUrlHandler implements MessageHandler<Object, String> {
	@Autowired
	private AccessControlService accessControlService;
	@Autowired
	private BucketService bucketService;
	@Autowired
	private MosServerProperties mosServerProperties;
	
	@Override
	public String handle(Object o, Object[] params, String mark) {
		String path = getParam(params, 0, String.class);
		Boolean isDir = getParam(params, 1, Boolean.class);
		if (isDir != null && isDir) {
			return null;
		}
		MosContext context = MosContext.getContext();
		Long bucketId = context.getBucketId();
		Bucket bucket = bucketService.findById(bucketId);
		Long currentUserId = context.getCurrentUserId();
		List<AccessControl> openIds = accessControlService.findOwnList(currentUserId, bucketId);
		if (CollectionUtils.isEmpty(openIds)) {
			return null;
		}
		AccessControl accessControl = openIds.get(0);
		
		String requestDomain = RequestUtils.getRequestDomain();
		Assert.notBlank(requestDomain, "requestDomain is blank");
		MosConfig mosConfig = new MosConfig(bucket.getBucketName(), accessControl.getSecretKey(), accessControl.getOpenId());
		try (MosSdk mosSdk = new MosSdk(mosConfig)) {
			UrlBuildParams urlBuildParams = UrlBuildParams.builder(path, 3600 * 5L, TimeUnit.SECONDS)
				.host(requestDomain)
				.render(true)
				.build();
			return mosSdk.getUrl(urlBuildParams);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
}
