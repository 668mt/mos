package mt.spring.mos.server.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.http.client.config.CookieSpecs;
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
		simpleClientHttpRequestFactory.setReadTimeout(0);
		return new RestTemplate(simpleClientHttpRequestFactory);
	}
	
	@Bean
	public ClientHttpRequestFactory httpRequestFactory(CloseableHttpClient httpClient) {
		return new HttpComponentsClientHttpRequestFactory(httpClient);
	}
	
	@Bean
	public CloseableHttpClient httpClient() {
		final RequestConfig requestConfig = RequestConfig.custom()
			.setConnectionRequestTimeout(5, TimeUnit.SECONDS)
			.setResponseTimeout(1, TimeUnit.HOURS)
			.setConnectTimeout(5, TimeUnit.SECONDS)
			.setCookieSpec(CookieSpecs.IGNORE_COOKIES)
			.build();
		PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
			.setMaxConnPerRoute(2000)
			.setMaxConnTotal(2000)
			.build();
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				connectionManager.closeExpired();
			}
		}, 30000, 5000);
		return HttpClients.custom()
			.setDefaultRequestConfig(requestConfig)
			.setConnectionManager(connectionManager)
			.disableRedirectHandling()
			.build();
	}
	
	@Bean(name = "backRestTemplate")
	public RestTemplate backRestTemplate() {
		SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
		simpleClientHttpRequestFactory.setConnectTimeout(10000);
		simpleClientHttpRequestFactory.setReadTimeout(0);
		return new RestTemplate(simpleClientHttpRequestFactory);
	}
}
