package mt.spring.mos.server.service.cron;

import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.po.FileHouse;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.service.FileHouseService;
import mt.spring.mos.server.service.ResourceService;
import mt.spring.mos.server.service.TaskScheduleService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author Martin
 * @Date 2021/1/7
 */
@Component
public class FileHouseCron extends BaseCron {
	public FileHouseCron(TaskScheduleService taskScheduleService) {
		super(taskScheduleService);
	}
	
	@Autowired
	private FileHouseService fileHouseService;
	@Autowired
	private MosServerProperties mosServerProperties;
	@Autowired
	private ResourceService resourceService;
	
	/**
	 * 定时清除不用的文件
	 */
	@Scheduled(cron = "${mos.cron.file-house.check:0 0 2 * * ?}")
	public void checkFileHouseAndDelete() {
		checkFileHouseAndDeleteRecent(mosServerProperties.getDeleteRecentDaysNotUsed(), true);
	}
	
	/**
	 * 删除不用的文件
	 *
	 * @param days              最近几天
	 * @param checkLastModified 是否校验最后修改时间
	 */
	public void checkFileHouseAndDeleteRecent(int days, boolean checkLastModified) {
		log.info("删除{}天前未使用的文件", days);
		List<FileHouse> notUsedFileHouseList = fileHouseService.findNotUsedFileHouseList(days);
		if (CollectionUtils.isEmpty(notUsedFileHouseList)) {
			log.info("没有要删除的文件！");
			return;
		}
		taskScheduleService.fragment(notUsedFileHouseList, FileHouse::getId, fileHouse -> {
			fileHouseService.clearFileHouse(fileHouse, checkLastModified);
		});
	}
	
//	/**
//	 * 转换传统资源为文件仓库
//	 */
//	@Scheduled(fixedDelayString = "${mos.traditional.convert.delay:30000}")
//	public void convertTraditionalToFileHouse() {
//		if (mosServerProperties.getConvertTraditionalToFileHouse() == null || !mosServerProperties.getConvertTraditionalToFileHouse()) {
//			return;
//		}
//		List<Resource> needConvertToFileHouse = resourceService.findNeedConvertToFileHouse(100);
//		taskScheduleService.fragment(needConvertToFileHouse, Resource::getId, resource -> {
//			try {
//				fileHouseService.convertTraditionalToFileHouse(resource);
//			} catch (Exception e) {
//				log.error(e.getMessage(), e);
//			}
//			if (mosServerProperties.getConvertTraditionalToFileHouseSleepMills() != null && mosServerProperties.getConvertTraditionalToFileHouseSleepMills() > 0) {
//				try {
//					Thread.sleep(mosServerProperties.getConvertTraditionalToFileHouseSleepMills());
//				} catch (InterruptedException e) {
//					log.error(e.getMessage(), e);
//				}
//			}
//		});
//	}
}
