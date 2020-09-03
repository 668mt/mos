package mt.spring.mos.server.service;

import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.dao.ClientMapper;
import mt.spring.mos.server.dao.RelaClientResourceMapper;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.RelaClientResource;
import mt.spring.mos.server.entity.po.Resource;
import mt.utils.Assert;
import mt.utils.MyUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2020/5/18
 */
@Service
public class ClientService extends BaseServiceImpl<Client> {
	@Autowired
	private ClientMapper clientMapper;
	@Autowired
	@Lazy
	private ResourceService resourceService;
	@Autowired
	private RelaClientResourceMapper relaClientResourceMapper;
	@Autowired
	private RestTemplate restTemplate;
	
	@Override
	public BaseMapper<Client> getBaseMapper() {
		return clientMapper;
	}
	
	public List<Client> findAvaliableClients() {
		return findByFilter(new Filter("status", Filter.Operator.eq, Client.ClientStatus.UP));
	}
	
	/**
	 * 随机查找可用的资源服务器，提供调用
	 *
	 * @param pathname  路径
	 * @param freeSpace 剩余空间大于这个存储
	 * @return
	 */
	public Client findRandomAvalibleClient(@Nullable String pathname, long freeSpace) {
		List<Client> avaliableClients = null;
		if (pathname == null) {
			avaliableClients = findAvaliableClients();
		} else {
			if (!pathname.startsWith("/")) {
				pathname = "/" + pathname;
			}
			Resource resource = resourceService.findOne("pathname", pathname);
			Assert.notNull(resource, "不存在此资源");
			List<RelaClientResource> relaClientResources = relaClientResourceMapper.findList("resourceId", resource.getId());
			Assert.notEmpty(relaClientResources, "不存在此资源");
			List<String> clientIds = relaClientResources.stream().map(RelaClientResource::getClientId).collect(Collectors.toList());
			List<Filter> filters = new ArrayList<>();
			filters.add(new Filter("clientId", Filter.Operator.in, clientIds));
			filters.add(new Filter("status", Filter.Operator.eq, Client.ClientStatus.UP));
			avaliableClients = findByFilters(filters);
			Assert.notEmpty(avaliableClients, "无可用的资源服务器");
		}
		if (MyUtils.isEmpty(avaliableClients)) {
			return null;
		}
		//使用少的排前面
		List<Client> clients = avaliableClients.stream().filter(client -> client.getTotalStorageByte() - client.getUsedStorageByte() > freeSpace).filter(this::isAlive).collect(Collectors.toList());
		Assert.state(MyUtils.isNotEmpty(clients), "无可用的资源服务器");
		return randomClient(clients);
	}
	
	public Client randomClient(@NotNull List<Client> clients) {
		Assert.notEmpty(clients, "clients不能为空");
		Random random = new Random();
		int priority = 0;
		for (Client client : clients) {
			client.setPriority_min(priority);
			priority += client.getWeight();
			client.setPriority_max(priority);
		}
		int i = random.nextInt(priority);
		for (Client client : clients) {
			if (client.getPriority_min() <= i && i < client.getPriority_max()) {
				return client;
			}
		}
		throw new RuntimeException("权重算法出错");
	}
	
	public boolean isAlive(Client client) {
		try {
			restTemplate.getForObject(client.getUrl() + "/actuator/info", String.class);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
