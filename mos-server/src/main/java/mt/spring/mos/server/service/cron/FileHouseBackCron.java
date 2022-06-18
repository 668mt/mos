package mt.spring.mos.server.service.cron;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.vo.BackVo;
import mt.spring.mos.server.service.FileHouseService;
import mt.spring.mos.server.service.TaskScheduleService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
	private final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(3, 3, 0, TimeUnit.MINUTES, new LinkedBlockingDeque<>(2000), r -> {
		Thread thread = new Thread(r);
		thread.setName("fileBackThread");
		return thread;
	}, new ThreadPoolExecutor.CallerRunsPolicy());
	
	@Scheduled(fixedDelay = 30 * 1000)
	public void checkBackFileHouse() {
		List<BackVo> needBackResources = fileHouseService.findNeedBackFileHouses(mosServerProperties.getBackCronLimit());
		if (CollectionUtils.isNotEmpty(needBackResources)) {
			taskScheduleService.fragment(needBackResources, BackVo::getFileHouseId, task -> {
				threadPoolExecutor.execute(() -> {
					try {
						fileHouseService.backFileHouse(task);
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				});
			});
		}
	}
	
}
