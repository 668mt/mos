package mt.spring.mos.server.entity.handler;

import mt.common.starter.message.messagehandler.MessageHandler;
import mt.spring.mos.base.utils.CollectionUtils;
import mt.spring.mos.sdk.MosSdk;
import mt.spring.mos.server.config.MosUserContext;
import mt.spring.mos.server.config.aop.MosContext;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.po.AccessControl;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.service.AccessControlService;
import mt.spring.mos.server.service.BucketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2021/8/21
 */
@Component
public class SignUrlHandler implements MessageHandler {
	@Autowired
	private AccessControlService accessControlService;
	@Autowired
	private MosServerProperties mosServerProperties;
	@Autowired
	private BucketService bucketService;
	
	@Override
	public Object handle(Object[] params, String mark) {
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
		MosSdk mosSdk = new MosSdk(mosServerProperties.getDomain(), accessControl.getOpenId(), bucket.getBucketName(), accessControl.getSecretKey());
		return mosSdk.getUrl(path, 3600, TimeUnit.SECONDS, mosServerProperties.getDomain(), true, false);
	}
}
