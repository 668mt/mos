package mt.spring.mos.server.service.clientapi;

import mt.spring.mos.server.entity.po.Client;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;

import static mt.spring.mos.server.config.AsyncConfiguration.DEFAULT_EXECUTOR_NAME;

/**
 * @Author Martin
 * @Date 2021/1/9
 */
@Component
public class ClientApiFactory {
	@Autowired
	@Qualifier("httpRestTemplate")
	private RestTemplate restTemplate;
	@Autowired
	private CloseableHttpClient httpClient;
	@Autowired
	@Qualifier(DEFAULT_EXECUTOR_NAME)
	private ExecutorService executorService;
	
	public IClientApi getClientApi(Client client) {
		return new ClientApi(client, restTemplate, httpClient, executorService);
	}
	
}
