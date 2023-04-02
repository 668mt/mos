package mt.spring.mos.server.service.cron;

import lombok.extern.slf4j.Slf4j;
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
	public void checkBackFileHouse() {
		List<BackVo> needBackResources = fileHouseService.findNeedBackFileHouses(mosServerProperties.getBackCronLimit());
		if (CollectionUtils.isNotEmpty(needBackResources)) {
			taskScheduleService.fragment(needBackResources, BackVo::getFileHouseId, task -> {
				try {
					fileHouseService.backFileHouse(task);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			});
		}
	}
	
}
