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
import mt.spring.mos.server.service.strategy.StrategyFactory;
import mt.utils.Assert;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
	private FileHouseRelaClientService fileHouseRelaClientService;
	@Autowired
	private StrategyFactory strategyFactory;
	
	@Override
	public BaseMapper<Client> getBaseMapper() {
		return clientMapper;
	}
	
	public List<Client> findAvaliableClients() {
		return findByFilter(new Filter("status", Filter.Operator.eq, Client.ClientStatus.UP));
	}
	
	public Client findRandomAvalibleClientForUpload(long freeSpace) {
		return strategyFactory.getDefaultClientStrategy().getClient(freeSpace);
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
			avaliableClients = findAvaliableClientByIds(clientIds);
		} else {
			Long fileHouseId = resource.getFileHouseId();
			List<FileHouseRelaClient> fileHouseRelaClients = fileHouseRelaClientService.findList("fileHouseId", fileHouseId);
			Assert.notNull(fileHouseRelaClients, "资源不存在");
			avaliableClients = fileHouseRelaClients.parallelStream().map(this::findById).filter(client -> client.getStatus() == Client.ClientStatus.UP).collect(Collectors.toList());
		}
		Assert.notEmpty(avaliableClients, "无可用的资源服务器");
		return strategyFactory.getDefaultClientStrategy().getClient(0, avaliableClients);
	}
	
	private List<Client> findAvaliableClientByIds(List<String> clientIds) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("clientId", Filter.Operator.in, clientIds));
		filters.add(new Filter("status", Filter.Operator.eq, Client.ClientStatus.UP));
		return findByFilters(filters);
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
