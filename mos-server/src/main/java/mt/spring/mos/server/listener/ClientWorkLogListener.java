package mt.spring.mos.server.listener;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.server.controller.discovery.RegistEvent;
import mt.spring.mos.server.entity.po.ClientWorkLog;
import mt.spring.mos.server.service.ClientWorkLogService;
import mt.spring.mos.server.service.cron.ClientWorkLogCron;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/10/23
 */
@Component
@Slf4j
public class ClientWorkLogListener {
	@Autowired
	private ClientWorkLogService clientWorkLogService;
	@Autowired
	private ClientWorkLogCron clientWorkLogCron;
	
	@EventListener
	public void listenRegist(RegistEvent registEvent) {
		Long clientId = registEvent.getClient().getId();
		List<ClientWorkLog> tasks = clientWorkLogService.findTasksByClientId(clientId);
		clientWorkLogCron.doClientWorkLogs(tasks);
	}
	
}
