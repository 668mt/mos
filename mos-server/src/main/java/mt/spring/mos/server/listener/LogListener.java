package mt.spring.mos.server.listener;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.server.controller.discovery.Instance;
import mt.spring.mos.server.controller.discovery.RegistEvent;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.ClientWorkLog;
import mt.spring.mos.server.service.ClientService;
import mt.spring.mos.server.service.ClientWorkLogService;
import mt.spring.mos.server.service.TaskScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2020/10/23
 */
@Component
@Slf4j
public class LogListener {
	@Autowired
	private ClientWorkLogService clientWorkLogService;
	@Autowired
	private TaskScheduleService taskScheduleService;
	@Autowired
	private ClientService clientService;
	@Autowired
	@Qualifier("httpRestTemplate")
	private RestTemplate httpRestTemplate;
	
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
		String clientId = clientWorkLogEvent.getClientId();
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
		Instance instance = registEvent.getInstance();
		String clientId = instance.getClientId();
		List<ClientWorkLog> tasks = clientWorkLogService.findTasksByClientId(clientId);
		doLogWork(tasks);
	}
	
	@Scheduled(fixedDelay = 10 * 1000)
	public void job() {
		List<ClientWorkLog> tasks = clientWorkLogService.findTasks();
		doLogWork(tasks);
	}
	
	private void doLogWork(List<ClientWorkLog> tasks) {
		taskScheduleService.fragment(tasks, ClientWorkLog::getId, task -> {
			synchronized (this) {
				task = clientWorkLogService.findById(task.getId());
				if (task.getExeStatus() != ClientWorkLog.ExeStatus.NOT_START) {
					return;
				}
				Client client = clientService.findOne("clientId", task.getClientId());
				try {
					switch (task.getAction()) {
						case ADD_FILE:
							task.setExeStatus(ClientWorkLog.ExeStatus.IGNORE);
							break;
						case DELETE_FILE:
							List<String> pathnames = (List<String>) task.getParams().get("pathnames");
							for (String pathname : pathnames) {
								client.apis(httpRestTemplate).deleteFile(pathname);
							}
							task.setExeStatus(ClientWorkLog.ExeStatus.SUCCESS);
							break;
						case DELETE_DIR:
							List<String> paths = (List<String>) task.getParams().get("paths");
							for (String path : paths) {
								client.apis(httpRestTemplate).deleteDir(path);
							}
							task.setExeStatus(ClientWorkLog.ExeStatus.SUCCESS);
							break;
						case MOVE_FILE:
							String srcPathname = (String) task.getParams().get("srcPathname");
							String desPathname = (String) task.getParams().get("desPathname");
							client.apis(httpRestTemplate).moveFile(srcPathname, desPathname);
							task.setExeStatus(ClientWorkLog.ExeStatus.SUCCESS);
							break;
					}
				} catch (Exception e) {
					task.setExeStatus(ClientWorkLog.ExeStatus.FAIL);
					task.setMessage(e.getMessage());
					log.error(e.getMessage(), e);
				}
				clientWorkLogService.updateById(task);
			}
		});
	}
}
