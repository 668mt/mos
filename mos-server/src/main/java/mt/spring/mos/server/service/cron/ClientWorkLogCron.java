package mt.spring.mos.server.service.cron;

import mt.spring.mos.server.entity.po.ClientWorkLog;
import mt.spring.mos.server.service.ClientWorkLogService;
import mt.spring.mos.server.service.TaskScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author Martin
 * @Date 2021/1/7
 */
@Component
public class ClientWorkLogCron extends BaseCron {
	@Autowired
	private ClientWorkLogService clientWorkLogService;
	
	public ClientWorkLogCron(TaskScheduleService taskScheduleService) {
		super(taskScheduleService);
	}
	
	@Scheduled(fixedDelay = 10 * 1000)
	public void doClientWorkLogsCron() {
		List<ClientWorkLog> tasks = clientWorkLogService.findTasks();
		doClientWorkLogs(tasks);
	}
	
	public void doClientWorkLogs(List<ClientWorkLog> tasks) {
		taskScheduleService.fragment(tasks, ClientWorkLog::getId, task -> {
			clientWorkLogService.doLogWork(task);
		});
	}
	
}
