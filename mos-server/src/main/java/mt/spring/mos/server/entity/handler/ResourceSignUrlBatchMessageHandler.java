package mt.spring.mos.server.entity.handler;

import mt.common.starter.message.messagehandler.BatchMessageHandler;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.base.utils.CollectionUtils;
import mt.spring.mos.sdk.MosSdk;
import mt.spring.mos.sdk.entity.MosConfig;
import mt.spring.mos.sdk.entity.params.UrlBuildParams;
import mt.spring.mos.server.config.aop.MosContext;
import mt.spring.mos.server.dao.ResourceMapper;
import mt.spring.mos.server.entity.bo.ResourcePathBO;
import mt.spring.mos.server.entity.po.AccessControl;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.service.AccessControlService;
import mt.spring.mos.server.service.BucketService;
import mt.spring.mos.server.utils.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2026/9/12
 */
@Component
public class ResourceSignUrlBatchMessageHandler implements BatchMessageHandler<Long, String> {
	@Autowired
	private BucketService bucketService;
	@Autowired
	private AccessControlService accessControlService;
	@Autowired
	private ResourceMapper resourceMapper;
	
	@Override
	public Map<Long, String> handle(Collection<?> collection, Set<Long> resourceIds, String[] params) {
		Map<Long, String> result = new HashMap<>();
		if (CollectionUtils.isEmpty(resourceIds)) {
			return result;
		}
		MosContext context = MosContext.getContext();
		if (context == null || context.getBucketId() == null) {
			return result;
		}
		Long bucketId = context.getBucketId();
		
		Bucket bucket = bucketService.findById(bucketId);
		Long currentUserId = context.getCurrentUserId();
		List<AccessControl> openIds = accessControlService.findOwnList(currentUserId, bucketId);
		if (CollectionUtils.isEmpty(openIds)) {
			return result;
		}
		AccessControl accessControl = openIds.get(0);
		List<ResourcePathBO> resourcePathBOS = resourceMapper.findResourcePaths(resourceIds);
		if (CollectionUtils.isEmpty(resourcePathBOS)) {
			return result;
		}
		
		String requestDomain = RequestUtils.getRequestDomain();
		Assert.notBlank(requestDomain, "requestDomain is blank");
		MosConfig mosConfig = new MosConfig(bucket.getBucketName(), accessControl.getSecretKey(), accessControl.getOpenId());
		try (MosSdk mosSdk = new MosSdk(mosConfig)) {
			for (ResourcePathBO resourcePathBO : resourcePathBOS) {
				UrlBuildParams urlBuildParams = UrlBuildParams.builder(resourcePathBO.getPath(), 3600 * 5L, TimeUnit.SECONDS)
					.host(requestDomain)
					.render(true)
					.build();
				String url = mosSdk.getUrl(urlBuildParams);
				result.put(resourcePathBO.getResourceId(), url);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}
}
