package mt.spring.mos.server.service.cron;

import lombok.extern.slf4j.Slf4j;
import mt.common.fragment.TaskFragment;
import mt.spring.mos.base.utils.CollectionUtils;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.po.UploadFile;
import mt.spring.mos.server.service.UploadFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author Martin
 * @Date 2023/9/9
 */
@Component
@Slf4j
public class UploadFileCron {
	@Autowired
	private UploadFileService uploadFileService;
	@Autowired
	private MosServerProperties mosServerProperties;
	@Autowired
	private TaskFragment taskFragment;
	
	/**
	 * 定时清除不用的文件
	 */
	@Scheduled(cron = "${mos.cron.upload-file.check:0 0 2 * * ?}")
	public void checkAndDelete() {
		checkAndDeleteRecent(mosServerProperties.getDeleteRecentDaysNotUsed());
	}
	
	public void checkAndDeleteRecent(int days) {
		log.info("删除{}天前未使用的文件", days);
		List<UploadFile> notUsedFileHouseList = uploadFileService.findNotUsedFileHouseList(days);
		if (CollectionUtils.isEmpty(notUsedFileHouseList)) {
			log.info("没有要删除的文件！");
			return;
		}
		taskFragment.fragment(notUsedFileHouseList, UploadFile::getId, uploadFile -> {
			uploadFileService.clearUploadFile(uploadFile.getId());
		});
	}
}
