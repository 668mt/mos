package mt.spring.mos.server.service.cron;

import mt.spring.mos.server.service.TaskScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author Martin
 * @Date 2021/1/7
 */
public class BaseCron {
	protected TaskScheduleService taskScheduleService;
	protected Logger log = LoggerFactory.getLogger(BaseCron.class);
	
	public BaseCron(TaskScheduleService taskScheduleService) {
		this.taskScheduleService = taskScheduleService;
	}
}
