package mt.spring.mos.server.service.strategy;

import mt.spring.mos.server.entity.MosServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/11/30
 */
@Component
public class StrategyFactory {
	@Autowired
	private MosServerProperties mosServerProperties;
	@Autowired
	private List<ClientStrategy> clientStrategies;
	
	public ClientStrategy getClientStrategy() {
		return clientStrategies.stream().filter(clientStrategy -> clientStrategy.getName().equalsIgnoreCase(mosServerProperties.getClientStrategy())).findFirst().orElseThrow(() -> new IllegalStateException("没有找到客户端策略：" + mosServerProperties.getClientStrategy()));
	}
}
