package mt.spring.mos.server.controller.discovery;

import lombok.extern.slf4j.Slf4j;
import mt.common.entity.ResResult;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.service.ClientService;
import mt.spring.mos.server.service.TaskScheduleService;
import mt.utils.Assert;
import mt.utils.MyUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.executor.ScheduledTasksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * @Author Martin
 * @Date 2020/6/7
 */
@RestController
@RequestMapping("/discovery")
@Slf4j
public class DiscoveryController {
	
	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;
	@Autowired
	private ClientService clientService;
	@Autowired
	private TaskScheduleService taskScheduleService;
	@Autowired
	private MosServerProperties mosServerProperties;
	private final Map<String, Boolean> isRegistMap = Collections.synchronizedMap(new HashMap<>());
	
	/**
	 * 接收心跳
	 *
	 * @param isRegist
	 * @return
	 */
	@PutMapping("/beat")
	public ResResult regist(@RequestParam(defaultValue = "false") Boolean isRegist, Instance instance) {
		if (StringUtils.isNotBlank(mosServerProperties.getRegistPwd())) {
			Assert.state(mosServerProperties.getRegistPwd().equals(instance.getRegistPwd()), "注册密码不匹配");
		}
		Client client = new Client();
		client.setClientId(instance.getClientId());
		client.setIp(instance.getIp());
		client.setPort(instance.getPort());
		client.setWeight(instance.getWeight());
		client.setRemark(instance.getRemark());
		client.setStatus(Client.ClientStatus.UP);
		client.setLastBeatTime(new Date());
		Client findClient = clientService.findById(client);
		if (findClient == null) {
			clientService.save(client);
		} else {
			clientService.updateByIdSelective(client);
		}
		Boolean isRegistServer = isRegistMap.get(client.getClientId());
		if (isRegist) {
			//注册
			isRegistMap.put(client.getClientId(), true);
			applicationEventPublisher.publishEvent(new RegistEvent(this, instance));
		} else {
			if (isRegistServer == null || !isRegistServer) {
				isRegistMap.put(client.getClientId(), true);
				applicationEventPublisher.publishEvent(new RegistEvent(this, instance));
			} else {
				//维持心跳
				applicationEventPublisher.publishEvent(new BeatEvent(this, instance));
			}
		}
		return ResResult.success();
	}
	
	@Scheduled(fixedDelay = 10 * 1000)
	public void health() {
		if (!taskScheduleService.isReady()) {
			return;
		}
		List<Client> all = clientService.findByFilter(new Filter("status", Filter.Operator.eq, Client.ClientStatus.UP));
		if (MyUtils.isEmpty(all)) {
			return;
		}
		taskScheduleService.fragment(all, task -> task.getClientId().hashCode(), client -> {
			if (client.getLastBeatTime() == null || client.getLastBeatTime().getTime() + 30 * 1000 < System.currentTimeMillis()) {
				log.info("{}服务不可用， 标记为下线", client.getClientId());
				client.setStatus(Client.ClientStatus.DOWN);
				clientService.updateByIdSelective(client);
				applicationEventPublisher.publishEvent(new ClientDownEvent(DiscoveryController.this, client));
			}
		});
	}
}
