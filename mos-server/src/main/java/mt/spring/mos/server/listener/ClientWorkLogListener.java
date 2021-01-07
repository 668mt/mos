package mt.spring.mos.server.listener;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.server.controller.discovery.RegistEvent;
import mt.spring.mos.server.entity.po.ClientWorkLog;
import mt.spring.mos.server.service.ClientWorkLogService;
import mt.spring.mos.server.service.cron.ClientWorkLogCron;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	public void listen(ClientWorkLogEvent clientWorkLogEvent) {
		Map<String, Object> params = new HashMap<>();
		Object[] args = clientWorkLogEvent.getArgs();
		ClientWorkLog.Action action = clientWorkLogEvent.getAction();
		switch (action) {
			case MOVE_FILE:
				params.put("srcPathname", args[0]);
				params.put("desPathname", args[1]);
				break;
			case ADD_FILE:
			case DELETE_FILE:
				params.put("pathnames", args);
				break;
			case DELETE_DIR:
				params.put("paths", args);
				break;
		}
		Long clientId = clientWorkLogEvent.getClientId();
		ClientWorkLog.ExeStatus exeStatus = clientWorkLogEvent.getExeStatus();
		ClientWorkLog clientWorkLog = new ClientWorkLog();
		clientWorkLog.setAction(action);
		clientWorkLog.setClientId(clientId);
		clientWorkLog.setExeStatus(exeStatus);
		clientWorkLog.setParams(params);
		clientWorkLogService.save(clientWorkLog);
	}
	
	@EventListener
	public void listenRegist(RegistEvent registEvent) {
		Long clientId = registEvent.getClient().getId();
		List<ClientWorkLog> tasks = clientWorkLogService.findTasksByClientId(clientId);
		clientWorkLogCron.doClientWorkLogs(tasks);
	}
	
}
