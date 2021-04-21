package mt.spring.mos.server.service.strategy;

import mt.spring.mos.base.utils.IpUtils;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.service.ClientService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 优先选择本地主机的client
 *
 * @Author Martin
 * @Date 2020/12/4
 */
@Component
public class CurrentPriorityWeightClientStragegy extends WeightClientStrategy {
	public static final String STRATEGY_NAME = "priority_weight";
	@Autowired
	private MosServerProperties mosServerProperties;
	
	public CurrentPriorityWeightClientStragegy(ClientService clientService) {
		super(clientService);
	}
	
	@Override
	public Client getClient(List<Client> clients) {
		String currentIp = mosServerProperties.getCurrentIp();
		if (StringUtils.isBlank(currentIp)) {
			currentIp = IpUtils.getHostIp(mosServerProperties.getIpPrefix());
		}
		String finalCurrentIp = currentIp;
		Optional<Client> currentClient = clients.stream().filter(client -> client.getIp().equalsIgnoreCase(finalCurrentIp)).findFirst();
		return currentClient.orElseGet(() -> super.getClient(clients));
	}
	
	@Override
	public String getName() {
		return STRATEGY_NAME;
	}
}
