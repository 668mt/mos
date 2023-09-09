package mt.spring.mos.server.service;

import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.FileHouseRelaClient;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.exception.NoAvailableClientBizException;
import mt.spring.mos.server.exception.NoThumbBizException;
import mt.spring.mos.server.exception.ResourceNotExistsException;
import mt.spring.mos.server.service.clientapi.ClientApiFactory;
import mt.spring.mos.server.service.strategy.StrategyFactory;
import mt.utils.common.Assert;
import mt.utils.common.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2020/5/18
 */
@Service
public class ClientService extends BaseServiceImpl<Client> {
	@Autowired
	@Lazy
	private ResourceService resourceService;
	@Autowired
	@Lazy
	private FileHouseRelaClientService fileHouseRelaClientService;
	@Autowired
	@Lazy
	private StrategyFactory strategyFactory;
	@Autowired
	private ClientApiFactory clientApiFactory;
	
	public List<Client> filterByFreeSpace(List<Client> clients, long freeSpace) {
		if (CollectionUtils.isEmpty(clients)) {
			return new ArrayList<>();
		}
		if (freeSpace <= 0) {
			return clients;
		}
		return clients.stream().filter(client -> client.getTotalStorageByte() - client.getUsedStorageByte() - client.getKeepSpaceByte() > freeSpace).filter(this::isAlive).collect(Collectors.toList());
	}
	
	public List<Client> findAvaliableClients() {
		return findByFilter(new Filter("status", Filter.Operator.eq, Client.ClientStatus.UP));
	}
	
	public Client findRandomAvalibleClientForUpload(long freeSpace) {
		return strategyFactory.getDefaultClientStrategy().getClient(freeSpace);
	}
	
	public Client findRandomAvalibleClientForVisit(@NotNull Long fileHouseId) {
		List<FileHouseRelaClient> fileHouseRelaClients = fileHouseRelaClientService.findList("fileHouseId", fileHouseId);
		if (CollectionUtils.isEmpty(fileHouseRelaClients)) {
			throw new ResourceNotExistsException("资源不存在，fileHouseId=" + fileHouseId);
		}
		List<Client> avaliableClients = fileHouseRelaClients.parallelStream().map(fileHouseRelaClient -> {
			Long clientId = fileHouseRelaClient.getClientId();
			return findById(clientId);
		}).filter(client -> client.getStatus() == Client.ClientStatus.UP).collect(Collectors.toList());
		if (CollectionUtils.isEmpty(avaliableClients)) {
			throw new NoAvailableClientBizException("无可用的资源服务器，fileHouseId=" + fileHouseId);
		}
		return strategyFactory.getDefaultClientStrategy().getClient(0, avaliableClients);
	}
	
	public Client findRandomAvalibleClientForVisit(Resource resource, boolean thumb) {
		Assert.notNull(resource, "不存在此资源");
		if (thumb) {
			if (resource.getThumbFileHouseId() == null) {
				throw new NoThumbBizException("资源" + resource.getName() + "无缩略图");
			}
			return findRandomAvalibleClientForVisit(resource.getThumbFileHouseId());
		} else {
			return findRandomAvalibleClientForVisit(resource.getFileHouseId());
		}
	}
	
	private List<Client> findAvaliableClientByIds(List<Long> clientIds) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("id", Filter.Operator.in, clientIds));
		filters.add(new Filter("status", Filter.Operator.eq, Client.ClientStatus.UP));
		return findByFilters(filters);
	}
	
	public Client findOneByName(String name) {
		return findOne("name", name);
	}
	
	public boolean isAlive(Client client) {
		return clientApiFactory.getClientApi(client).isAlive();
	}
	
	@Transactional
	public void kick(Long id) {
		Client client = lock(id);
		Assert.notNull(client, "客户端不能为空");
		client.setStatus(Client.ClientStatus.KICKED);
		updateByIdSelective(client);
	}
	
	@Transactional
	public void recover(Long id) {
		Client client = lock(id);
		Assert.notNull(client, "客户端不能为空");
		Assert.state(client.getStatus() == Client.ClientStatus.KICKED, "服务器" + id + "未被剔除，不能进行恢复");
		client.setStatus(isAlive(client) ? Client.ClientStatus.UP : Client.ClientStatus.DOWN);
		updateByIdSelective(client);
	}
	
	@Transactional(propagation = Propagation.MANDATORY)
	public Client lock(long clientId) {
		return findOneByFilter(new Filter("id", Filter.Operator.eq, clientId), true);
	}
	
}
