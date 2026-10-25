package mt.spring.mos.client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * @Author Martin
 * @Date 2020/5/18
 */
@Configuration
public class ClientRestTemplateConfig {
	
	@Bean(name = "registerRestTemplate")
	@Primary
	public RestTemplate registerRestTemplate() {
		SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
		simpleClientHttpRequestFactory.setConnectTimeout(10000);
		simpleClientHttpRequestFactory.setReadTimeout(60000);
		return new RestTemplate(simpleClientHttpRequestFactory);
	}
}
