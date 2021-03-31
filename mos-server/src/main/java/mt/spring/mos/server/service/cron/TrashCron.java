package mt.spring.mos.server.service.cron;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.server.service.DirService;
import mt.spring.mos.server.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @Author Martin
 * @Date 2021/3/28
 */
@Component
@Slf4j
public class TrashCron {
	@Autowired
	private DirService dirService;
	@Autowired
	private ResourceService resourceService;
	
	@Scheduled(cron = "0 0 3 * * ?")
	public void realDeleteCron() {
		//删除15天前的文件
		log.info("清空15天前的回收站...");
		deleteTrashBeforeDays(15);
	}
	
	public void deleteTrashBeforeDays(int days) {
		dirService.realDeleteDirBefore(days);
		resourceService.realDeleteResourceBefore(days);
	}
}
