package mt.spring.mos.server.service.cron;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.vo.BackVo;
import mt.spring.mos.server.service.FileHouseService;
import mt.spring.mos.server.service.TaskScheduleService;
import mt.utils.executor.MtExecutor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/5/18
 */
@Service
@Slf4j
public class FileHouseBackCron {
	@Autowired
	private TaskScheduleService taskScheduleService;
	@Autowired
	private FileHouseService fileHouseService;
	@Autowired
	private MosServerProperties mosServerProperties;
	
	private final MtExecutor<BackVo> backResouceExecutor = new MtExecutor<BackVo>(2) {
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
	
	@Scheduled(fixedDelay = 5 * 60 * 1000)
	public void checkBackFileHouse() {
		List<BackVo> needBackResources = fileHouseService.findNeedBackFileHouses(mosServerProperties.getBackCronLimit());
		if (CollectionUtils.isNotEmpty(needBackResources)) {
			taskScheduleService.fragment(needBackResources, BackVo::getFileHouseId, task -> {
				if (!backResouceExecutor.contains(task)) {
					backResouceExecutor.submit(task);
				}
			});
		}
	}
	
}
