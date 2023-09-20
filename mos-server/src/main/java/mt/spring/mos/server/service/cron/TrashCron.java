package mt.spring.mos.server.service.cron;

import lombok.extern.slf4j.Slf4j;
import mt.common.config.log.TraceContext;
import mt.common.fragment.TaskFragment;
import mt.spring.mos.base.utils.CollectionUtils;
import mt.spring.mos.server.entity.po.Dir;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.service.DirService;
import mt.spring.mos.server.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author Martin
 * @Date 2021/3/28
 */
@Component
@Slf4j
public class TrashCron extends BaseCron {
	@Autowired
	private DirService dirService;
	@Autowired
	private ResourceService resourceService;
	
	public TrashCron(TaskFragment taskFragment) {
		super(taskFragment);
	}
	
	@Scheduled(cron = "0 0 3 * * ?")
	public void realDeleteCron() {
		TraceContext.setTraceId(TraceContext.getOrCreate());
		//删除15天前的文件
		log.info("清空15天前的回收站...");
		deleteTrashBeforeDays(15, true);
	}
	
	public void deleteTrashBeforeDays(int days, boolean fragment) {
		List<Dir> realDeleteDirsBefore = dirService.getRealDeleteDirsBefore(days);
		List<Resource> realDeleteResourceBefore = resourceService.getRealDeleteResourceBefore(days);
		if (fragment) {
			taskFragment.fragment(realDeleteDirsBefore, Dir::getId, dir -> dirService.realDeleteDir(dir.getBucketId(), dir.getId()));
			taskFragment.fragment(realDeleteResourceBefore, Resource::getId, resource -> resourceService.realDeleteResource(resource));
		} else {
			if (CollectionUtils.isNotEmpty(realDeleteDirsBefore)) {
				realDeleteDirsBefore.forEach(dir -> dirService.realDeleteDir(dir.getBucketId(), dir.getId()));
			}
			if (CollectionUtils.isNotEmpty(realDeleteResourceBefore)) {
				realDeleteResourceBefore.forEach(resource -> resourceService.realDeleteResource(resource));
			}
		}
	}
}
