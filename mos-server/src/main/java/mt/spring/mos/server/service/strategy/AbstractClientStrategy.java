package mt.spring.mos.server.service.strategy;

import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.service.ClientService;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/11/30
 */
public abstract class AbstractClientStrategy implements ClientStrategy {
	private final ClientService clientService;
	
	public AbstractClientStrategy(ClientService clientService) {
		this.clientService = clientService;
	}
	
	@Override
	public Client getClient(long freeSpace) {
		return getClient(freeSpace, null);
	}
	
	@Override
	public Client getClient(long freeSpace, @Nullable List<Client> avaliableClients) {
		if (avaliableClients == null) {
			avaliableClients = clientService.findAvaliableClients();
		}
		List<Client> clients = clientService.filterByFreeSpace(avaliableClients, freeSpace);
		Assert.notEmpty(clients, "无可用资源服务器");
		return getClient(clients);
	}
	
	public abstract Client getClient(List<Client> clients);
}
