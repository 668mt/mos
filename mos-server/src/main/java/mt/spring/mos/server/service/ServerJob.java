package mt.spring.mos.server.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.controller.discovery.RegistEvent;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.FileHouse;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.entity.vo.BackVo;
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
import java.util.concurrent.ExecutionException;

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
	@Autowired
	private FileHouseService fileHouseService;
	@Autowired
	private MosServerProperties mosServerProperties;
	
	private final MtExecutor<BackVo> backResouceExecutor = new MtExecutor<BackVo>(5) {
		@Override
		public void doJob(BackVo task) {
			if (!taskScheduleService.isCurrentJob(task, taskId -> task.getFileHouseId())) {
				return;
			}
			try {
				fileHouseService.backFileHouse(task);
			} catch (IllegalArgumentException | IllegalStateException e1) {
				log.warn(e1.getMessage());
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	};
//	private final MtExecutor<WaitingImportResource> importExecutor = new MtExecutor<WaitingImportResource>(5) {
//		@Override
//		public void doJob(WaitingImportResource task) {
//			if (!taskScheduleService.isCurrentJob(task, o -> task.getPathname().hashCode())) {
//				return;
//			}
//			Long bucketId = task.getBucketId();
//			Bucket bucket = bucketService.findById(bucketId);
//			if (bucket == null) {
//				log.debug("bucket{}不存在", bucketId);
//				return;
//			}
//			Resource resource = new Resource();
//			resource.setPathname(task.getPathname());
//			resource.setSizeByte(task.getSizeByte());
//			resourceService.addResourceIfNotExist(resource, task.getClient().getClientId(), bucketId);
//		}
//	};
	
	@Override
	public void afterPropertiesSet() throws Exception {
		backResouceExecutor.startAlways();
//		importExecutor.startAlways();
	}
	
	@Scheduled(fixedDelay = 5 * 60 * 1000)
	@Async
	public void checkBackFileHouse() {
		List<BackVo> needBackResources = fileHouseService.findNeedBackFileHouses(mosServerProperties.getBackCronLimit());
		if (MyUtils.isNotEmpty(needBackResources)) {
			taskScheduleService.fragment(needBackResources, BackVo::getFileHouseId, task -> {
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
	
	/**
	 * 注册事件监听
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	@EventListener
	public void clientRegistHandle(RegistEvent registEvent) {
		String clientId = registEvent.getInstance().getClientId();
		log.info("{}注册服务!", clientId);
		
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
			checkFreeSpace();
		}).start();
	}
	
	/**
	 * 定时清除不用的文件
	 */
	@Scheduled(cron = "${mos.cron.file-house.check:0 0 2 * * ?}")
	public void checkFileHouseAndDelete() {
		checkFileHouseAndDeleteRecent(mosServerProperties.getDeleteRecentDaysNotUsed(), true);
	}
	
	public void checkFileHouseAndDeleteRecent(int days, boolean checkLastModified) {
		log.info("删除{}天前未使用的文件", days);
		List<FileHouse> notUsedFileHouseList = fileHouseService.findNotUsedFileHouseList(days);
		if (CollectionUtils.isEmpty(notUsedFileHouseList)) {
			log.info("没有要删除的文件！");
			return;
		}
		taskScheduleService.waitUntilReady();
		taskScheduleService.fragment(notUsedFileHouseList, FileHouse::getId, fileHouse -> {
			fileHouseService.clearFileHouse(fileHouse, checkLastModified);
		});
	}
	
	/**
	 * 转换传统资源为文件仓库
	 */
	@Scheduled(fixedDelayString = "${mos.traditional.convert.delay:30000}")
	public void convertTraditionalToFileHouse() {
		if (mosServerProperties.getConvertTraditionalToFileHouse() == null || !mosServerProperties.getConvertTraditionalToFileHouse()) {
			return;
		}
		List<Resource> needConvertToFileHouse = resourceService.findNeedConvertToFileHouse(100);
		taskScheduleService.waitUntilReady();
		taskScheduleService.fragment(needConvertToFileHouse, Resource::getId, resource -> {
			try {
				fileHouseService.convertTraditionalToFileHouse(resource);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			if (mosServerProperties.getConvertTraditionalToFileHouseSleepMills() != null && mosServerProperties.getConvertTraditionalToFileHouseSleepMills() > 0) {
				try {
					Thread.sleep(mosServerProperties.getConvertTraditionalToFileHouseSleepMills());
				} catch (InterruptedException e) {
					log.error(e.getMessage(), e);
				}
			}
		});
	}
	
	@Scheduled(fixedDelayString = "${mos.schedule.generate.thumb:30000}")
	public void generateThumb() {
		List<Resource> resources = resourceService.findNeedGenerateThumb(100);
		taskScheduleService.fragment(resources, Resource::getId, resource -> {
			try {
				resourceService.createThumb(resource).get();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		});
	}
	
}
