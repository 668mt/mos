package mt.spring.mos.client.service;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.utils.IpUtils;
import mt.spring.mos.client.entity.MosClientProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author Martin
 * @Date 2020/6/8
 */
@Service
@Slf4j
public class RegistSchedule {
	private final AtomicBoolean isRegister = new AtomicBoolean(true);
	@Autowired
	@Qualifier("httpRestTemplate")
	private RestTemplate httpRestTemplate;
	@Value("${spring.application.name:mos-client}")
	private String serviceId;
	@Value("${server.port:9800}")
	private Integer port;
	@Autowired
	private MosClientProperties mosClientProperties;
	@Autowired
	private ClientService clientService;
	private final AtomicReference<String> lastRegistSuccessHost = new AtomicReference<>();
	private MosClientProperties.Instance singleInstance;
	
	public MosClientProperties.Instance getInstance() {
		if (singleInstance == null) {
			synchronized (this) {
				if (singleInstance == null) {
					singleInstance = mosClientProperties.getInstance();
					if (StringUtils.isBlank(singleInstance.getIp())) {
						String ip = IpUtils.getHostIp(singleInstance.getIpPrefix());
						singleInstance.setIp(ip);
					}
					if (singleInstance.getPort() == null) {
						singleInstance.setPort(port);
					}
					if (StringUtils.isBlank(singleInstance.getName())) {
						singleInstance.setName(singleInstance.getIp() + ":" + serviceId + ":" + singleInstance.getPort());
					}
				}
			}
		}
		return singleInstance;
	}
	
	@Scheduled(fixedRate = 20_000L)
	public void registerCron() {
		if (!register()) {
			log.error("注册失败，无可用的注册地址：" + Arrays.toString(mosClientProperties.getServerHosts()));
		}
	}
	
	private boolean register() {
		String lastSuccessHost = lastRegistSuccessHost.get();
		if (StringUtils.isNotBlank(lastSuccessHost)) {
			//上次成功注册的地址
			if (register(lastSuccessHost)) {
				return true;
			}
		}
		for (String serverHost : mosClientProperties.getServerHosts()) {
			if (lastSuccessHost != null && lastSuccessHost.equals(serverHost)) {
				continue;
			}
			if (register(serverHost)) {
				lastRegistSuccessHost.set(serverHost);
				return true;
			}
		}
		return false;
	}
	
	private boolean register(String host) {
		try {
			MosClientProperties.Instance instance = getInstance();
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
			params.add("isRegister", isRegister);
			params.add("name", instance.getName());
			params.add("ip", instance.getIp());
			params.add("port", instance.getPort());
			params.add("weight", instance.getWeight());
			params.add("remark", instance.getRemark());
			params.add("minAvaliableSpaceGB", mosClientProperties.getMinAvaliableSpaceGB());
			if (StringUtils.isNotBlank(mosClientProperties.getRegistPwd())) {
				params.add("registPwd", mosClientProperties.getRegistPwd());
			}
			if(clientService.isHealth()){
				params.add("status", "UP");
			}else{
				params.add("status", "DOWN");
			}
			HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(params, httpHeaders);
			ResponseEntity<String> response = httpRestTemplate.exchange(host + "/discovery/beat", HttpMethod.PUT, httpEntity, String.class);
			log.debug("注册结果：{}", response.getBody());
			if (isRegister.get()) {
				log.info(instance.getName() + "注册成功!");
				isRegister.set(false);
			}
			return true;
		} catch (Exception e) {
			log.error("发送心跳失败：" + host + "," + e.getMessage(), e);
			return false;
		}
	}
}
