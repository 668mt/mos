//package mt.spring.mos.server.service.cron;
//
//import mt.spring.mos.server.entity.po.Resource;
//import mt.spring.mos.server.service.ResourceService;
//import mt.spring.mos.server.service.TaskScheduleService;
//import mt.spring.mos.server.service.ThumbService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
///**
// * 自动生成缩略图
// *
// * @Author Martin
// * @Date 2021/1/7
// */
//@Component
//public class ThumbCron extends BaseCron {
//	@Autowired
//	private ResourceService resourceService;
//	@Autowired
//	private ThumbService thumbService;
//	/**
//	 * 是否自动生成缩略图
//	 */
//	@Value("${mos.schedule.generateThumb:true}")
//	private Boolean generateThumb;
//
//	public ThumbCron(TaskScheduleService taskScheduleService) {
//		super(taskScheduleService);
//	}
//
//	/**
//	 * 自动生成缩略图
//	 */
//	@Scheduled(fixedDelayString = "${mos.schedule.generate.thumb:300000}")
//	public void generateThumb() {
//		if (!generateThumb) {
//			return;
//		}
//		List<Resource> resources = thumbService.findNeedGenerateThumb(100);
//		taskScheduleService.fragment(resources, Resource::getId, resource -> {
//			try {
//				if (!taskScheduleService.isCurrentJob(resource, Resource::getId)) {
//					return;
//				}
//				thumbService.createThumb(resource.getId()).get();
//			} catch (Exception e) {
//				log.error(e.getMessage(), e);
//			}
//		});
//	}
//}
