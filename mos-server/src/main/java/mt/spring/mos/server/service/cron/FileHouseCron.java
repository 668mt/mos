package mt.spring.mos.server.service.cron;

import mt.common.config.log.TraceContext;
import mt.common.fragment.TaskFragment;
import mt.spring.mos.base.utils.CollectionUtils;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.po.FileHouse;
import mt.spring.mos.server.service.FileHouseService;
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
	public FileHouseCron(TaskFragment taskFragment) {
		super(taskFragment);
	}
	
	@Autowired
	private FileHouseService fileHouseService;
	@Autowired
	private MosServerProperties mosServerProperties;
	
	/**
	 * 定时清除不用的文件
	 */
	@Scheduled(cron = "${mos.cron.file-house.check:0 0 2 * * ?}")
	public void checkFileHouseAndDelete() {
		TraceContext.setTraceId(TraceContext.getOrCreate());
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
		taskFragment.fragment(notUsedFileHouseList, FileHouse::getId, fileHouse -> {
			fileHouseService.clearFileHouse(fileHouse, checkLastModified);
		});
	}
	
}
