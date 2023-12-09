package mt.spring.mos.server.service;

import lombok.extern.slf4j.Slf4j;
import mt.common.fragment.TaskFragment;
import mt.spring.mos.server.controller.discovery.BeatEvent;
import mt.spring.mos.server.controller.discovery.ClientDownEvent;
import mt.spring.mos.server.controller.discovery.Instance;
import mt.spring.mos.server.controller.discovery.RegistEvent;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.po.Client;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务发现
 *
 * @Author Martin
 * @Date 2023/4/2
 */
@Service
@Slf4j
public class DiscoveryService {
	@Autowired
	private MosServerProperties mosServerProperties;
	private final Map<String, Boolean> isRegistMap = new ConcurrentHashMap<>();
	@Autowired
	private ClientService clientService;
	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;
	@Autowired
	private LockService lockService;
	@Autowired
	private TaskFragment taskFragment;
	
	/**
	 * 接收心跳，客户端每20秒发送一次
	 *
	 * @param isRegister 是否注册
	 * @param instance   实例
	 */
	public void register(boolean isRegister, @NotNull Instance instance) {
		if (StringUtils.isNotBlank(mosServerProperties.getRegistPwd())) {
			Assert.state(mosServerProperties.getRegistPwd().equals(instance.getRegistPwd()), "注册密码不匹配");
		}
		String clientName = instance.getName();
		lockService.doWithLock("client:" + clientName, LockService.LockType.WRITE, () -> {
			//客户端信息
			Client client = new Client();
			client.setName(clientName);
			client.setIp(instance.getIp());
			client.setPort(instance.getPort());
			client.setWeight(instance.getWeight());
			client.setRemark(instance.getRemark());
			if (instance.getStatus() != null) {
				client.setStatus(instance.getStatus());
			} else {
				client.setStatus(Client.ClientStatus.UP);
			}
			client.setLastBeatTime(new Date());
			if (instance.getMinAvaliableSpaceGB() != null) {
				//保留空间
				client.setKeepSpaceByte(instance.getMinAvaliableSpaceGB() * FileUtils.ONE_GB);
			} else {
				client.setKeepSpaceByte(0L);
			}
			//判断客户端是否存在
			Client findClient = clientService.findOneByName(clientName);
			if (findClient == null) {
				//不存在则新增
				clientService.save(client);
			} else {
				//存在则更新
				client.setId(findClient.getId());
				if (findClient.getStatus() == Client.ClientStatus.KICKED) {
					//被踢了
					client.setStatus(Client.ClientStatus.KICKED);
				}
				clientService.updateByIdSelective(client);
			}
			
			//是否注册过当前主机
			Boolean isRegisterServer = isRegistMap.get(client.getName());
			if (isRegister) {
				//注册
				isRegistMap.put(client.getName(), true);
				applicationEventPublisher.publishEvent(new RegistEvent(this, client));
			} else {
				if (isRegisterServer == null || !isRegisterServer) {
					//注册
					isRegistMap.put(client.getName(), true);
					applicationEventPublisher.publishEvent(new RegistEvent(this, client));
				} else {
					//维持心跳
					applicationEventPublisher.publishEvent(new BeatEvent(this, client));
				}
			}
		});
	}
	
	/**
	 * 健康检查
	 */
	@Scheduled(fixedDelay = 20 * 1000)
	@Async
	public void health() {
		log.debug("健康检查中...");
		List<Client> all = clientService.findAvaliableClients();
		if (CollectionUtils.isEmpty(all)) {
			return;
		}
		taskFragment.fragment(all, Client::getId, client -> {
			if (client.getStatus() != Client.ClientStatus.UP) {
				return;
			}
			long now = System.currentTimeMillis();
			if (client.getLastBeatTime() == null || client.getLastBeatTime().getTime() + 60 * 1000 < now) {
				if (!clientService.isAlive(client)) {
					log.info("{}服务不可用， 标记为下线", client.getName());
					client.setStatus(Client.ClientStatus.DOWN);
					clientService.updateByIdSelective(client);
					applicationEventPublisher.publishEvent(new ClientDownEvent(this, client));
				}
			}
		});
	}
}
