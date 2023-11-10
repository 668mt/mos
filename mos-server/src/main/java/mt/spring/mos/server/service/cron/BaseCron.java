package mt.spring.mos.server.service.cron;

import mt.common.fragment.TaskFragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author Martin
 * @Date 2021/1/7
 */
public class BaseCron {
	protected TaskFragment taskFragment;
	protected Logger log = LoggerFactory.getLogger(BaseCron.class);
	
	public BaseCron(TaskFragment taskFragment) {
		this.taskFragment = taskFragment;
	}
	
	
}
