package mt.spring.mos.server.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.server.config.RedisUtils;
import mt.utils.ReflectUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author Martin
 * @date 2020/5/29
 */
@SuppressWarnings("unchecked")
@Slf4j
public class TaskScheduleService {
	
	@Setter
	private RedisUtils redisUtils;
	@Setter
	private RestTemplate restTemplate;
	@Setter
	private ExecutorService executorService = Executors.newScheduledThreadPool(5);
	@Setter
	private String scheduleName = "TaskScheduleServices";
	private final String currentInstanceId;
	private final String currentHealchCheckUrl;
	private final Timer timer = new Timer(false);
	
	public TaskScheduleService(String scheduleName, String currentInstanceId, String currentHealchCheckUrl, RedisUtils redisUtils) {
		this(currentInstanceId, currentHealchCheckUrl, redisUtils);
		this.scheduleName = scheduleName;
	}
	
	public TaskScheduleService(String currentInstanceId, String currentHealchCheckUrl, RedisUtils redisUtils) {
		this.currentInstanceId = currentInstanceId;
		this.currentHealchCheckUrl = currentHealchCheckUrl;
		this.redisUtils = redisUtils;
		this.restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new ResponseErrorHandler() {
			@Override
			public boolean hasError(ClientHttpResponse response) throws IOException {
				return false;
			}
			
			@Override
			public void handleError(ClientHttpResponse response) throws IOException {
			
			}
		});
		startRegist();
	}
	
	public boolean isReady() {
		try {
			getCurrentFragmentInfo();
			return true;
		} catch (Exception ignored) {
			return false;
		}
	}
	
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	@Data
	public static class RegistInfo {
		private String healthCheckUrl;
		private String instanceId;
		private long registTime;
		private int expireSeconds = 20;
		
		@JsonIgnore
		public boolean isExpire() {
			return System.currentTimeMillis() - registTime > expireSeconds * 1000;
		}
	}
	
	protected List<RegistInfo> getServices() {
		List<RegistInfo> services = null;
		try {
			services = (List<RegistInfo>) redisUtils.get(scheduleName);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		if (services == null) {
			services = new CopyOnWriteArrayList<>();
		}
		return services;
	}
	
	protected void setServices(List<RegistInfo> services) {
		redisUtils.set(scheduleName, services, 60 * 1000);
	}
	
	@Data
	public class HealthCheckTask implements Runnable {
		private RegistInfo service;
		private RestTemplate restTemplate;
		private ExecutorService executorService;
		
		public HealthCheckTask(RegistInfo registInfo, RestTemplate restTemplate, ExecutorService executorService) {
			this.service = registInfo;
			this.restTemplate = restTemplate;
			this.executorService = executorService;
		}
		
		@Override
		public void run() {
			if (service.getInstanceId().equalsIgnoreCase(currentInstanceId)) {
				return;
			}
			Future<?> submit = executorService.submit(() -> {
				restTemplate.getForObject(service.getHealthCheckUrl(), String.class);
				log.debug("健康检查{}成功", service.getHealthCheckUrl());
			});
			try {
				submit.get(10000, TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				log.error("{}已失效", service.getInstanceId());
				service.setRegistTime(0);
			}
		}
	}
	
	public void waitUntilReady() {
		while (!isReady()) {
			try {
				log.info("等待taskSchedule初始化...");
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				log.error(e.getMessage(), e);
			}
		}
	}
	
	private void healthCheck() {
		List<RegistInfo> services = getServices();
		List<Future<?>> list = new ArrayList<>();
		for (RegistInfo service : services) {
			if (!service.isExpire()) {
				list.add(executorService.submit(new HealthCheckTask(service, restTemplate, executorService)));
			}
		}
		for (Future<?> future : list) {
			try {
				future.get();
			} catch (InterruptedException | ExecutionException e) {
				log.error(e.getMessage(), e);
			}
		}
		setServices(services);
	}
	
	private void startRegist() {
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				List<RegistInfo> services = getServices();
				List<RegistInfo> collect = new ArrayList<>();
				for (RegistInfo service : services) {
					if (currentInstanceId.equals(service.getInstanceId())) {
						collect.add(service);
					}
				}
				if (CollectionUtils.isNotEmpty(collect)) {
					RegistInfo registInfo = collect.get(0);
					registInfo.setRegistTime(System.currentTimeMillis());
					registInfo.setInstanceId(currentInstanceId);
				} else {
					RegistInfo registInfo = new RegistInfo();
					registInfo.setRegistTime(System.currentTimeMillis());
					registInfo.setInstanceId(currentInstanceId);
					registInfo.setHealthCheckUrl(currentHealchCheckUrl);
					services.add(registInfo);
				}
				setServices(services);
			}
		}, 0, 10 * 1000);
	}
	
	@Data
	public static class CurrentFragmentInfo {
		private int index;
		private int total;
	}
	
	private List<RegistInfo> getAvaliableList() {
		List<RegistInfo> services = getServices();
		return services.stream().filter(registInfo -> !registInfo.isExpire()).collect(Collectors.toList());
	}
	
	public interface Function<T> {
		void doJob(T task);
	}
	
	public interface FragmentIdFunction<T> {
		long getFragmentId(T task);
	}
	
	public CurrentFragmentInfo getCurrentFragmentInfo() {
		CurrentFragmentInfo currentFragmentInfo = new CurrentFragmentInfo();
		List<RegistInfo> avaliableList = getAvaliableList();
		currentFragmentInfo.setTotal(avaliableList.size());
		int i = 0;
		for (RegistInfo avaliableService : avaliableList) {
			if (currentInstanceId.equals(avaliableService.getInstanceId())) {
				currentFragmentInfo.setIndex(i);
				return currentFragmentInfo;
			}
			i++;
		}
		throw new IllegalStateException("当前服务未注册");
	}
	
	public interface ExceptionHandler<T> {
		void handleException(T task, Exception e);
	}
	
	public <T> void fragment(Collection<T> tasks, FragmentIdFunction<T> fragmentIdFunction, Function<T> function) {
		fragment(tasks, fragmentIdFunction, function, (task, e) -> log.error(e.getMessage(), e));
	}
	
	public <T> void fragment(Collection<T> tasks, FragmentIdFunction<T> fragmentIdFunction, Function<T> function, ExceptionHandler<T> exceptionHandler) {
		if (CollectionUtils.isEmpty(tasks)) {
			return;
		}
		waitUntilReady();
		healthCheck();
		CurrentFragmentInfo currentFragmentInfo = getCurrentFragmentInfo();
		int index = currentFragmentInfo.getIndex();
		int count = currentFragmentInfo.getTotal();
		for (T task : tasks) {
			long fragmentId = fragmentIdFunction.getFragmentId(task);
			if (fragmentId < 0) {
				fragmentId = -1 * fragmentId;
			}
			if (fragmentId % count == index) {
				try {
					function.doJob(task);
				} catch (Exception e) {
					exceptionHandler.handleException(task, e);
				}
			}
		}
	}
	
	public <T> void fragmentByFieldValue(Collection<T> tasks, String fieldName, Function<T> function) {
		fragment(tasks, task -> {
			Object value = ReflectUtils.getValue(task, fieldName, Object.class);
			return value == null ? 0 : Long.parseLong(value.toString());
		}, function);
	}
	
	public <T> void fragmentByFieldHashCode(Collection<T> tasks, String fieldName, Function<T> function) {
		fragment(tasks, task -> {
			Object value = ReflectUtils.getValue(task, fieldName, Object.class);
			return value == null ? 0 : value.hashCode();
		}, function);
	}
	
	public <T> void fragmentByValue(Collection<T> taskIds, Function<T> function) {
		fragment(taskIds, val -> val == null ? 0 : Long.parseLong(val.toString()), function);
	}
	
	public <T> boolean isCurrentJob(T task, FragmentIdFunction<T> fragmentIdFunction) {
		CurrentFragmentInfo currentFragmentInfo = getCurrentFragmentInfo();
		int index = currentFragmentInfo.getIndex();
		int count = currentFragmentInfo.getTotal();
		long fragmentId = fragmentIdFunction.getFragmentId(task);
		return fragmentId % count == index;
	}
	
}
