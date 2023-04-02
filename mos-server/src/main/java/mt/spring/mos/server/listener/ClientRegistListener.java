package mt.spring.mos.server.listener;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.server.controller.discovery.RegistEvent;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.service.cron.ClientCron;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @Author Martin
 * @Date 2021/1/7
 */
@Slf4j
@Component
public class ClientRegistListener {
	@Autowired
	private ClientCron clientCron;
	
	/**
	 * 注册事件监听
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	@EventListener
	public void clientRegistHandle(RegistEvent registEvent) {
		log.info("{}注册服务!", registEvent.getClient().getName());
		
		new Thread(() -> {
//			Client client = clientService.findById(clientId);
//			Client.ClientApi api = client.apis(restTemplate);
//			if (api.isEnableImport()) {
//				List list = api.getClientResources();
//				if (CollectionUtils.isNotEmpty(list)) {
//					//任务分片
//					taskScheduleService.fragment(list, o -> {
//						Map<String, Object> resourceInfo = (Map<String, Object>) o;
//						return resourceInfo.get("pathname").hashCode();
//					}, o -> {
//						Map<String, Object> resourceInfo = (Map<String, Object>) o;
//						String pathname = resourceInfo.get("pathname").toString();
//						Object sizeByte = resourceInfo.get("sizeByte");
//						pathname = pathname.replace("\\", "/");
//						if (pathname.startsWith("/")) {
//							pathname = pathname.substring(1);
//						}
//						String[] split = pathname.split("/");
//						if (split.length <= 1) {
//							return;
//						}
//						String firstDir = split[0];
//						Long bucketId;
//						Assert.state(firstDir.matches("\\d+"), firstDir + "文件夹不是bucketId");
//						bucketId = Long.parseLong(split[0]);
//						pathname = pathname.substring((bucketId + "").length() + 1);
//
//						WaitingImportResource waitingImportResource = new WaitingImportResource();
//						waitingImportResource.setBucketId(bucketId);
//						waitingImportResource.setPathname(pathname);
//						waitingImportResource.setSizeByte(Long.parseLong(sizeByte.toString()));
//						waitingImportResource.setClient(client);
////						if (!importExecutor.contains(waitingImportResource)) {
////							importExecutor.addQueue(waitingImportResource);
////						}
//					});
//				}
//			}
			clientCron.checkFreeSpace();
		}).start();
	}
	
	@Data
	public static class WaitingImportResource {
		private Long bucketId;
		private String pathname;
		private long sizeByte;
		private Client client;
		
		@Override
		public int hashCode() {
			return Objects.hash(bucketId, pathname);
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof WaitingImportResource)) {
				return false;
			}
			WaitingImportResource resource = (WaitingImportResource) o;
			return Objects.equals(resource.getBucketId(), getBucketId())
					&& Objects.equals(resource.getPathname(), getPathname());
		}
	}
}
