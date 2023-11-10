package mt.spring.mos.server.service.cron;

import lombok.extern.slf4j.Slf4j;
import mt.common.config.log.TraceContext;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.po.FileHouseDeleteLog;
import mt.spring.mos.server.service.DeleteLogService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author Martin
 * @Date 2023/10/6
 */
@Component
@Slf4j
public class DeleteLogCron {
	@Autowired
	private DeleteLogService deleteLogService;
	@Autowired
	private MosServerProperties mosServerProperties;
	
	/**
	 * 定时清除不用的文件
	 */
	@Scheduled(cron = "${mos.cron.file-house.check:0 0 1 * * ?}")
	public void checkFileHouseAndDelete() {
		TraceContext.setTraceId(TraceContext.getOrCreate());
		checkFileHouseAndDeleteRecent(mosServerProperties.getDeleteRecentDaysNotUsed());
	}
	
	public void checkFileHouseAndDeleteRecent(int days) {
		int limit = 5000;
		List<FileHouseDeleteLog> fileHouseDeleteLogs;
		do {
			fileHouseDeleteLogs = deleteLogService.findListBeforeDaysLimit(days, limit);
			log.info("删除{}天前未使用的文件，文件数：{}", days, fileHouseDeleteLogs.size());
			if (CollectionUtils.isEmpty(fileHouseDeleteLogs)) {
				break;
			}
			for (FileHouseDeleteLog fileHouseDeleteLog : fileHouseDeleteLogs) {
				deleteLogService.clear(fileHouseDeleteLog);
			}
		} while (fileHouseDeleteLogs.size() == limit);
	}
}
