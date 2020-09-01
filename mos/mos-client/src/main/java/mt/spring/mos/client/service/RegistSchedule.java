package mt.spring.mos.client.service;

import mt.spring.mos.client.entity.OssClientProperties;
import mt.spring.mos.client.utils.IpUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

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
	@Value("${spring.application.name:oss-client}")
	private String serviceId = "oss-client";
	@Value("${server.port:9800}")
	private Integer port = 9800;
	@Autowired
	private OssClientProperties ossClientProperties;
	private Timer registTimer = null;
	
	@EventListener
	public void regist(ContextRefreshedEvent contextRefreshedEvent) {
		if (registTimer != null) {
			return;
		}
		registTimer = new Timer();
		registTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				String[] serverHosts = ossClientProperties.getServerHosts();
				if (serverHosts == null) {
					log.error("未配置服务端地址，oss.cleint.server-hosts");
					return;
				}
				try {
					for (String serverHost : serverHosts) {
						regist(serverHost, ossClientProperties.getInstance());
					}
				} catch (Exception e) {
					log.error("注册服务" + StringUtils.join(serverHosts) + "失败：" + e.getMessage());
				}
			}
		}, 2000, 10000);
	}
	
	@SneakyThrows
	private void regist(String host, OssClientProperties.Instance instance) {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		if (StringUtils.isBlank(instance.getIp())) {
			String ip = IpUtils.getHostIp();
			instance.setIp(ip);
		}
		if (instance.getPort() == null) {
			instance.setPort(port);
		}
		if (StringUtils.isBlank(instance.getClientId())) {
			instance.setClientId(instance.getIp() + ":" + serviceId + ":" + instance.getPort());
		}
		MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
		params.add("isRegist", isRegist);
		params.add("clientId", instance.getClientId());
		params.add("ip", instance.getIp());
		params.add("port", instance.getPort());
		params.add("weight", instance.getWeight());
		params.add("remark", instance.getRemark());
		if (StringUtils.isNotBlank(ossClientProperties.getRegistPwd())) {
			params.add("registPwd", ossClientProperties.getRegistPwd());
		}
		HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(params, httpHeaders);
		ResponseEntity<String> response = httpRestTemplate.exchange(host + "/discovery/beat", HttpMethod.PUT, httpEntity, String.class);
		log.debug("注册结果：{}", response.getBody());
		if (isRegist.get()) {
			isRegist.set(false);
		}
	}
}
