package mt.spring.mos.server.service.cron;

import lombok.extern.slf4j.Slf4j;
import mt.common.config.log.TraceContext;
import mt.common.fragment.TaskFragment;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.vo.BackVo;
import mt.spring.mos.server.service.FileHouseService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
	private TaskFragment taskScheduleService;
	@Autowired
	private FileHouseService fileHouseService;
	@Autowired
	private MosServerProperties mosServerProperties;
	
	@Scheduled(fixedDelay = 30 * 1000)
	public void doCheckBackFileHouseJob() {
		try {
			TraceContext.setTraceId(TraceContext.getOrCreate());
			checkBackFileHouse(true);
		} catch (Exception e) {
			log.error("checkBackFileHouse failed:{}", e.getMessage(), e);
		}
	}
	
	
	public synchronized void checkBackFileHouse(boolean checkFree) throws InterruptedException {
//		Double backCpuIdePercent = mosServerProperties.getBackCpuIdePercent();
//		if (checkFree && !SystemMonitor.hasCpuFreePercent(backCpuIdePercent)) {
//			log.info("cpu空闲使用率不足{}，暂停备份", backCpuIdePercent);
//			return;
//		}
		log.info("开始备份任务");
		List<BackVo> needBackResources = fileHouseService.findNeedBackFileHouses(mosServerProperties.getBackCronLimit());
		if (CollectionUtils.isNotEmpty(needBackResources)) {
			log.info("开始备份{}个文件", needBackResources.size());
			//计数器
//			AtomicInteger atomicInteger = new AtomicInteger(1);
			taskScheduleService.fragment(needBackResources, BackVo::getFileHouseId, task -> {
				try {
//					if (atomicInteger.getAndIncrement() % 10 == 0) {
//						if (checkFree && !SystemMonitor.hasCpuFreePercent(backCpuIdePercent)) {
//							log.info("cpu空闲使用率不足{}，暂停备份", backCpuIdePercent);
//							return;
//						}
//					}
					
					fileHouseService.backFileHouse(task);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			});
		}
	}
	
}
