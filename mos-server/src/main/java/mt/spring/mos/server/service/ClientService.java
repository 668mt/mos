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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
		return findRandomAvalibleClientForVisit(resource, false);
	}
	
	public Client findRandomAvalibleClientForVisit(@NotNull Long fileHouseId) {
		List<FileHouseRelaClient> fileHouseRelaClients = fileHouseRelaClientService.findList("fileHouseId", fileHouseId);
		Assert.notNull(fileHouseRelaClients, "资源不存在");
		List<Client> avaliableClients = fileHouseRelaClients.parallelStream().map(this::findById).filter(client -> client.getStatus() == Client.ClientStatus.UP).collect(Collectors.toList());
		Assert.notEmpty(avaliableClients, "无可用的资源服务器");
		return strategyFactory.getDefaultClientStrategy().getClient(0, avaliableClients);
	}
	
	public Client findRandomAvalibleClientForVisit(Resource resource, boolean thumb) {
		Assert.notNull(resource, "不存在此资源");
		List<Client> avaliableClients;
		if (resource.getFileHouseId() == null) {
			List<RelaClientResource> relaClientResources = relaClientResourceMapper.findList("resourceId", resource.getId());
			Assert.notEmpty(relaClientResources, "不存在此资源");
			List<String> clientIds = relaClientResources.stream().map(RelaClientResource::getClientId).collect(Collectors.toList());
			avaliableClients = findAvaliableClientByIds(clientIds);
			Assert.notEmpty(avaliableClients, "无可用的资源服务器");
			return strategyFactory.getDefaultClientStrategy().getClient(0, avaliableClients);
		} else {
			if (thumb) {
				Assert.notNull(resource.getThumbFileHouseId(), "资源" + resource.getPathname() + "无缩略图");
				return findRandomAvalibleClientForVisit(resource.getThumbFileHouseId());
			} else {
				return findRandomAvalibleClientForVisit(resource.getFileHouseId());
			}
		}
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
	
	@Autowired
	private RedissonClient redissonClient;
	
	@Transactional
	public void kick(String clientId) {
		RLock lock = redissonClient.getLock(clientId);
		try {
			lock.lock(2, TimeUnit.MINUTES);
			Client client = findById(clientId);
			Assert.notNull(client, "客户端不能为空");
			client.setStatus(Client.ClientStatus.KICKED);
			updateByIdSelective(client);
		} finally {
			lock.unlock();
		}
	}
	
	@Transactional
	public void recover(String clientId) {
		RLock lock = redissonClient.getLock(clientId);
		try {
			lock.lock(2, TimeUnit.MINUTES);
			Client client = findById(clientId);
			Assert.notNull(client, "客户端不能为空");
			Assert.state(client.getStatus() == Client.ClientStatus.KICKED, "服务器" + clientId + "未被剔除，不能进行恢复");
			client.setStatus(isAlive(client) ? Client.ClientStatus.UP : Client.ClientStatus.DOWN);
			updateByIdSelective(client);
		} finally {
			lock.unlock();
		}
	}
}
