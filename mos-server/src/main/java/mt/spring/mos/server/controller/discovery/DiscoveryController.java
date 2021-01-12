package mt.spring.mos.server.controller.discovery;

import lombok.extern.slf4j.Slf4j;
import mt.common.entity.ResResult;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.service.ClientService;
import mt.spring.mos.server.service.TaskScheduleService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
	@Autowired
	private RedissonClient redissonClient;
	private final Map<String, Boolean> isRegistMap = new ConcurrentHashMap<>();
	
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
		client.setName(instance.getName());
		client.setIp(instance.getIp());
		client.setPort(instance.getPort());
		client.setWeight(instance.getWeight());
		client.setRemark(instance.getRemark());
		client.setStatus(Client.ClientStatus.UP);
		client.setLastBeatTime(new Date());
		if (instance.getMinAvaliableSpaceGB() != null) {
			client.setKeepSpaceByte(instance.getMinAvaliableSpaceGB() * FileUtils.ONE_GB);
		} else {
			client.setKeepSpaceByte(0L);
		}
		Client findClient = clientService.findOneByName(client.getName());
		if (findClient == null) {
			clientService.save(client);
		} else {
			client.setId(findClient.getId());
			if (findClient.getStatus() == Client.ClientStatus.KICKED) {
				return ResResult.error("当前节点已被剔除");
			}
			RLock lock = redissonClient.getLock("client:" + client.getName());
			try {
				lock.lock(2, TimeUnit.MINUTES);
				if (findClient.getStatus() == Client.ClientStatus.KICKED) {
					return ResResult.error("当前节点已被剔除");
				} else {
					clientService.updateByIdSelective(client);
				}
			} finally {
				lock.unlock();
			}
		}
		Boolean isRegistServer = isRegistMap.get(client.getName());
		if (isRegist) {
			//注册
			isRegistMap.put(client.getName(), true);
			applicationEventPublisher.publishEvent(new RegistEvent(this, client));
		} else {
			if (isRegistServer == null || !isRegistServer) {
				isRegistMap.put(client.getName(), true);
				applicationEventPublisher.publishEvent(new RegistEvent(this, client));
			} else {
				//维持心跳
				applicationEventPublisher.publishEvent(new BeatEvent(this, client));
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
		if (CollectionUtils.isEmpty(all)) {
			return;
		}
		taskScheduleService.fragment(all, Client::getId, client -> {
			if (client.getStatus() != Client.ClientStatus.UP) {
				return;
			}
			if (client.getLastBeatTime() == null || client.getLastBeatTime().getTime() + 30 * 1000 < System.currentTimeMillis()) {
				log.info("{}服务不可用， 标记为下线", client.getName());
				client.setStatus(Client.ClientStatus.DOWN);
				clientService.updateByIdSelective(client);
				applicationEventPublisher.publishEvent(new ClientDownEvent(DiscoveryController.this, client));
			}
		});
	}
}
