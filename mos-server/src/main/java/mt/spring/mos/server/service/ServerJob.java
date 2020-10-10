package mt.spring.mos.server.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.controller.discovery.RegistEvent;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.Resource;
import mt.utils.MtExecutor;
import mt.utils.MyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @Author Martin
 * @Date 2020/5/18
 */
@Service
@Slf4j
public class ServerJob implements InitializingBean {
	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private ClientService clientService;
	@Autowired
	private ResourceService resourceService;
	@Autowired
	private BucketService bucketService;
	@Autowired
	private UserService userService;
	@Autowired
	private TaskScheduleService taskScheduleService;
	private final MtExecutor<Long> backResouceExecutor = new MtExecutor<Long>(5) {
		@Override
		public void doJob(Long task) {
			if (!taskScheduleService.isCurrentJob(task, taskId -> taskId)) {
				return;
			}
			try {
				resourceService.backResource(task);
			} catch (IllegalArgumentException | IllegalStateException e1) {
				log.warn(e1.getMessage());
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	};
	private final MtExecutor<WaitingImportResource> importExecutor = new MtExecutor<WaitingImportResource>(5) {
		@Override
		public void doJob(WaitingImportResource task) {
			if (!taskScheduleService.isCurrentJob(task, o -> task.getPathname().hashCode())) {
				return;
			}
			Long userId = task.getUserId();
			Long bucketId = task.getBucketId();
			if (!userService.existsId(userId)) {
				log.debug("用户{}不存在", userId);
				return;
			}
			Bucket bucket = bucketService.findById(bucketId);
			if (bucket == null) {
				log.debug("bucket{}不存在", bucketId);
				return;
			}
			Resource resource = new Resource();
			resource.setPathname(task.getPathname());
			resource.setSizeByte(task.getSizeByte());
			resourceService.addResourceIfNotExist(resource, task.getClient().getClientId(), bucketId);
		}
	};
	
	@Override
	public void afterPropertiesSet() throws Exception {
		backResouceExecutor.startAlways();
		importExecutor.startAlways();
	}
	
	@Scheduled(fixedDelay = 5 * 60 * 1000)
	@Async
	public void checkBackResource() {
		List<Long> needBackResources = resourceService.findNeedBackResources();
		if (MyUtils.isNotEmpty(needBackResources)) {
			taskScheduleService.fragmentByValue(needBackResources, task -> {
				if (!backResouceExecutor.getQueue().contains(task)) {
					backResouceExecutor.addQueue(task);
				}
			});
		}
	}
	
	/**
	 * 检查各主机磁盘可用空间
	 */
	@Scheduled(fixedDelay = 5 * 60 * 1000)
	@SuppressWarnings({"rawtypes"})
	public void checkFreeSpace() {
		List<Client> all = clientService.findByFilter(new Filter("status", Filter.Operator.eq, Client.ClientStatus.UP));
		if (all == null) {
			return;
		}
		
		for (Client client : all) {
			Map info = restTemplate.getForObject("http://" + client.getIp() + ":" + client.getPort() + "/client/info", Map.class);
			if (info != null) {
				try {
					Map spaceInfo = (Map) info.get("spaceInfo");
					Long totalSpace = Long.parseLong(spaceInfo.get("totalSpace").toString());
					Long freeSpace = Long.parseLong(spaceInfo.get("freeSpace").toString());
					client.setTotalStorageByte(totalSpace);
					client.setUsedStorageByte(totalSpace - freeSpace);
					clientService.updateByIdSelective(client);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}
	}
	
	@Data
	public static class WaitingImportResource {
		private Long bucketId;
		private Long userId;
		private String pathname;
		private long sizeByte;
		private Client client;
		
		@Override
		public int hashCode() {
			return Objects.hash(userId, bucketId, pathname);
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof WaitingImportResource)) {
				return false;
			}
			WaitingImportResource resource = (WaitingImportResource) o;
			return Objects.equals(resource.getBucketId(), getBucketId())
					&& Objects.equals(resource.getPathname(), getPathname())
					&& Objects.equals(resource.getUserId(), getUserId());
		}
	}
	
	/**
	 * 注册事件监听
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	@EventListener
	public void clientRegistHandle(RegistEvent registEvent) {
		String clientId = registEvent.getInstance().getClientId();
		log.info("{}注册服务!", clientId);
		
		new Thread(() -> {
			Client client = clientService.findById(clientId);
			Client.ClientApi api = client.apis(restTemplate);
			if (api.isEnableImport()) {
				List list = api.getClientResources();
				if (CollectionUtils.isNotEmpty(list)) {
					//任务分片
					taskScheduleService.fragment(list, o -> {
						Map<String, Object> resourceInfo = (Map<String, Object>) o;
						return resourceInfo.get("pathname").hashCode();
					}, o -> {
						Map<String, Object> resourceInfo = (Map<String, Object>) o;
						String pathname = resourceInfo.get("pathname").toString();
						Object sizeByte = resourceInfo.get("sizeByte");
						pathname = pathname.replace("\\", "/");
						if (pathname.startsWith("/")) {
							pathname = pathname.substring(1);
						}
						String[] split = pathname.split("/");
						if (split.length <= 2) {
							return;
						}
						String firstDir = split[0];
						Long bucketId;
						Long userId;
						if (firstDir.matches("\\d+")) {
							userId = Long.parseLong(firstDir);
							bucketId = Long.parseLong(split[1]);
							pathname = pathname.substring((userId + "/" + bucketId).length() + 1);
						} else {
							Bucket bucket = bucketService.findOne("bucketName", firstDir);
							if (bucket == null) {
								return;
							}
							bucketId = bucket.getId();
							userId = bucket.getUserId();
						}
						
						WaitingImportResource waitingImportResource = new WaitingImportResource();
						waitingImportResource.setBucketId(bucketId);
						waitingImportResource.setUserId(userId);
						waitingImportResource.setPathname(pathname);
						waitingImportResource.setSizeByte(Long.parseLong(sizeByte.toString()));
						waitingImportResource.setClient(client);
						if (!importExecutor.contains(waitingImportResource)) {
							importExecutor.addQueue(waitingImportResource);
						}
					});
				}
			}
			checkFreeSpace();
		}).start();
	}
	
}
