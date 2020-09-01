package mt.spring.mos.server.config;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2020/5/18
 */
@Configuration
public class RestTemplateConfig {
	
	@Bean(name = "httpRestTemplate")
	@Primary
	public RestTemplate httpRestTemplate() {
		SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
		simpleClientHttpRequestFactory.setConnectTimeout(10000);
		simpleClientHttpRequestFactory.setReadTimeout(300000);
		return new RestTemplate(simpleClientHttpRequestFactory);
	}
	
	@Bean
	public ClientHttpRequestFactory httpRequestFactory(CloseableHttpClient httpClient) {
		return new HttpComponentsClientHttpRequestFactory(httpClient);
	}
	
	@Bean
	public HttpClientConnectionManager connectionManager(ApacheHttpClientConnectionManagerFactory connectionManagerFactory) {
		HttpClientConnectionManager connectionManager = connectionManagerFactory.newConnectionManager(
				true,
				200,
				200,
				-1, TimeUnit.MILLISECONDS,
				null);
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				connectionManager.closeExpiredConnections();
			}
		}, 30000, 5000);
		return connectionManager;
	}
	
	@Bean
	public CloseableHttpClient httpClient(HttpClientConnectionManager connectionManager) {
		final RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(5000)
				.setSocketTimeout(3600000)
				.setConnectTimeout(5000)
				.setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();
		return HttpClients.custom().setDefaultRequestConfig(requestConfig)
				.setConnectionManager(connectionManager).disableRedirectHandling()
				.build();
	}
	
	@Bean(name = "backRestTemplate")
	public RestTemplate backRestTemplate(ClientHttpRequestFactory httpRequestFactory) {
		return new RestTemplate(httpRequestFactory);
	}
}
