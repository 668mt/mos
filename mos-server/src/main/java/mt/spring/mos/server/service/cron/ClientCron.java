package mt.spring.mos.server.service.cron;

import mt.common.tkmapper.Filter;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.service.ClientService;
import mt.spring.mos.server.service.TaskScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2021/1/7
 */
@Component
public class ClientCron extends BaseCron {
	public ClientCron(TaskScheduleService taskScheduleService) {
		super(taskScheduleService);
	}
	
	@Autowired
	private ClientService clientService;
	@Autowired
	private RestTemplate restTemplate;
	
	
	/**
	 * 检查各主机磁盘可用空间
	 */
	@Scheduled(fixedDelay = 5 * 60 * 1000)
	@SuppressWarnings({"rawtypes"})
	public void checkFreeSpace() {
		List<Client> all = clientService.findByFilter(new Filter("status", Filter.Operator.eq, Client.ClientStatus.UP));
		if (all == null) {
			return;
		}
		
		for (Client client : all) {
			Map info = restTemplate.getForObject("http://" + client.getIp() + ":" + client.getPort() + "/client/info", Map.class);
			if (info != null) {
				try {
					Map spaceInfo = (Map) info.get("spaceInfo");
					Long totalSpace = Long.parseLong(spaceInfo.get("totalSpace").toString());
					Long freeSpace = Long.parseLong(spaceInfo.get("freeSpace").toString());
					client.setTotalStorageByte(totalSpace);
					client.setUsedStorageByte(totalSpace - freeSpace);
					clientService.updateByIdSelective(client);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}
	}
}
