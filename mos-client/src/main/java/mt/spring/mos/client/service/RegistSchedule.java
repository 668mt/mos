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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

    @Scheduled(fixedRate = 10_000L)
    public void registCron() {
        if (!regist()) {
            log.error("注册失败，无可用的注册地址：" + Arrays.toString(mosClientProperties.getServerHosts()));
        }
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

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    private boolean regist(String host) {
        try {
            Future<?> future = executorService.submit(() -> {
                MosClientProperties.Instance instance = getInstance();
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
                params.add("isRegist", isRegist);
                params.add("name", instance.getName());
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
                    log.info(instance.getName() + "注册成功!");
                    isRegist.set(false);
                }
            });
            future.get(5, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.error("发送心跳失败：" + host + "," + e.getMessage(), e);
            return false;
        }
    }
}
