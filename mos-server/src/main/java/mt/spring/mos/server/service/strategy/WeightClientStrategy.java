package mt.spring.mos.server.service.strategy;

import mt.spring.mos.base.algorithm.weight.WeightAlgorithm;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.service.ClientService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/11/30
 */
@Component
public class WeightClientStrategy extends AbstractClientStrategy {
	public static final String STRATEGY_NAME = "weight";
	
	public WeightClientStrategy(ClientService clientService) {
		super(clientService);
	}
	
	@Override
	public Client getClient(List<Client> clients) {
		return new WeightAlgorithm<Client>(clients).weightRandom();
	}
	
	@Override
	public String getName() {
		return STRATEGY_NAME;
	}
}
