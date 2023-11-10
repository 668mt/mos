package mt.spring.mos.server.service.clientapi;

import mt.spring.mos.server.entity.po.Client;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final Map<String, IClientApi> cacheMap = new ConcurrentHashMap<>();
    @Autowired
    @Qualifier(DEFAULT_EXECUTOR_NAME)
    private ExecutorService executorService;

    public IClientApi getClientApi(Client client) {
        IClientApi iClientApi = cacheMap.get(client.getName());
        if (iClientApi == null) {
            synchronized (this) {
                iClientApi = cacheMap.get(client.getName());
                if (iClientApi == null) {
                    iClientApi = new ClientApi(client, restTemplate, httpClient, executorService);
                    cacheMap.put(client.getName(), iClientApi);
                }
            }
        }
        return iClientApi;
    }

}
