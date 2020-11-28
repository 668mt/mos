package mt.spring.mos.server.service;

import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.dao.ClientMapper;
import mt.spring.mos.server.dao.RelaClientResourceMapper;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.FileHouseRelaClient;
import mt.spring.mos.server.entity.po.RelaClientResource;
import mt.spring.mos.server.entity.po.Resource;
import mt.utils.Assert;
import mt.utils.MyUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
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
	@Autowired
	@Lazy
	private FileHouseService fileHouseService;
	@Autowired
	@Lazy
	private FileHouseRelaClientService fileHouseRelaClientService;
	
	@Override
	public BaseMapper<Client> getBaseMapper() {
		return clientMapper;
	}
	
	public List<Client> findAvaliableClients() {
		return findByFilter(new Filter("status", Filter.Operator.eq, Client.ClientStatus.UP));
	}
	
	public Client findRandomAvalibleClientForUpload(long freeSpace) {
		List<Client> avaliableClients = findAvaliableClients();
		Assert.state(MyUtils.isNotEmpty(avaliableClients), "无可用的资源服务器");
		//使用少的排前面
		List<Client> clients = avaliableClients.stream().filter(client -> client.getTotalStorageByte() - client.getUsedStorageByte() > freeSpace).filter(this::isAlive).collect(Collectors.toList());
		Assert.state(MyUtils.isNotEmpty(clients), "无可用的资源服务器");
		return randomClient(clients);
	}
	
	public Client findRandomAvalibleClientForVisit(@NotNull Long bucketId, @NotNull String pathname) {
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		Assert.notNull(bucketId, "bucket不能为空");
		Resource resource = resourceService.findResourceByPathnameAndBucketId(pathname, bucketId);
		Assert.notNull(resource, "不存在此资源");
		List<Client> avaliableClients;
		if (resource.getFileHouseId() == null) {
			List<RelaClientResource> relaClientResources = relaClientResourceMapper.findList("resourceId", resource.getId());
			Assert.notEmpty(relaClientResources, "不存在此资源");
			List<String> clientIds = relaClientResources.stream().map(RelaClientResource::getClientId).collect(Collectors.toList());
			List<Filter> filters = new ArrayList<>();
			filters.add(new Filter("clientId", Filter.Operator.in, clientIds));
			filters.add(new Filter("status", Filter.Operator.eq, Client.ClientStatus.UP));
			avaliableClients = findByFilters(filters);
			Assert.notEmpty(avaliableClients, "无可用的资源服务器");
			//使用少的排前面
		} else {
			Long fileHouseId = resource.getFileHouseId();
			List<FileHouseRelaClient> fileHouseRelaClients = fileHouseRelaClientService.findList("fileHouseId", fileHouseId);
			Assert.notNull(fileHouseRelaClients, "资源不存在");
			avaliableClients = fileHouseRelaClients.parallelStream().map(this::findById).filter(this::isAlive).collect(Collectors.toList());
		}
		List<Client> clients = avaliableClients.stream().filter(client -> client.getTotalStorageByte() - client.getUsedStorageByte() > 0).filter(this::isAlive).collect(Collectors.toList());
		Assert.state(MyUtils.isNotEmpty(clients), "无可用的资源服务器");
		return randomClient(clients);
	}
	
	private List<Client> findAvaliableClientByIds(List<String> clientIds) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("clientId", Filter.Operator.in, clientIds));
		filters.add(new Filter("status", Filter.Operator.eq, Client.ClientStatus.UP));
		return findByFilters(filters);
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
	
	@Transactional
	public void kick(String clientId) {
		Client client = findById(clientId);
		Assert.notNull(client, "客户端不能为空");
		client.setStatus(Client.ClientStatus.KICKED);
		updateByIdSelective(client);
	}
	
	@Transactional
	public void recover(String clientId) {
		Client client = findById(clientId);
		Assert.notNull(client, "客户端不能为空");
		Date lastBeatTime = client.getLastBeatTime();
		if (lastBeatTime != null && System.currentTimeMillis() - lastBeatTime.getTime() < 20 * 1000) {
			client.setStatus(Client.ClientStatus.UP);
		} else {
			client.setStatus(Client.ClientStatus.DOWN);
		}
		updateByIdSelective(client);
	}
}
