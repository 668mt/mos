package mt.spring.mos.client.service;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.client.entity.MosClientProperties;
import mt.spring.mos.base.utils.IpUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author Martin
 * @Date 2020/6/8
 */
@Service
@Slf4j
public class RegistSchedule {
	private final AtomicBoolean isRegist = new AtomicBoolean(true);
	@Autowired
	@Qualifier("httpRestTemplate")
	private RestTemplate httpRestTemplate;
	@Value("${spring.application.name:mos-client}")
	private String serviceId;
	@Value("${server.port:9800}")
	private Integer port;
	@Autowired
	private MosClientProperties mosClientProperties;
	private Timer registTimer = null;
	private final AtomicReference<String> lastRegistSuccessHost = new AtomicReference<>();
	private MosClientProperties.Instance singleInstance;
	
	public MosClientProperties.Instance getInstance() {
		if (singleInstance == null) {
			synchronized (this) {
				if (singleInstance == null) {
					singleInstance = mosClientProperties.getInstance();
					if (StringUtils.isBlank(singleInstance.getIp())) {
						String ip = IpUtils.getHostIp();
						singleInstance.setIp(ip);
					}
					if (singleInstance.getPort() == null) {
						singleInstance.setPort(port);
					}
					if (StringUtils.isBlank(singleInstance.getClientId())) {
						singleInstance.setClientId(singleInstance.getIp() + ":" + serviceId + ":" + singleInstance.getPort());
					}
				}
			}
		}
		return singleInstance;
	}
	
	@EventListener
	public void regist(ContextRefreshedEvent contextRefreshedEvent) {
		if (registTimer != null) {
			return;
		}
		registTimer = new Timer();
		registTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (!regist()) {
					log.error("注册失败，无可用的注册地址：" + Arrays.toString(mosClientProperties.getServerHosts()));
				}
			}
		}, 2000, 10000);
	}
	
	private boolean regist() {
		boolean success;
		if (StringUtils.isNotBlank(lastRegistSuccessHost.get())) {
			success = regist(lastRegistSuccessHost.get());
			if (success) {
				return true;
			}
		}
		for (String serverHost : mosClientProperties.getServerHosts()) {
			success = regist(serverHost);
			if (success) {
				lastRegistSuccessHost.set(serverHost);
				return true;
			}
		}
		return false;
	}
	
	private boolean regist(String host) {
		try {
			MosClientProperties.Instance instance = getInstance();
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
			params.add("isRegist", isRegist);
			params.add("clientId", instance.getClientId());
			params.add("ip", instance.getIp());
			params.add("port", instance.getPort());
			params.add("weight", instance.getWeight());
			params.add("remark", instance.getRemark());
			params.add("minAvaliableSpaceGB", mosClientProperties.getMinAvaliableSpaceGB());
			if (StringUtils.isNotBlank(mosClientProperties.getRegistPwd())) {
				params.add("registPwd", mosClientProperties.getRegistPwd());
			}
			HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(params, httpHeaders);
			ResponseEntity<String> response = httpRestTemplate.exchange(host + "/discovery/beat", HttpMethod.PUT, httpEntity, String.class);
			log.debug("注册结果：{}", response.getBody());
			if (isRegist.get()) {
				isRegist.set(false);
			}
			return true;
		} catch (Exception e) {
			log.error("发送心跳失败：" + host + "," + e.getMessage(), e);
			return false;
		}
	}
}
