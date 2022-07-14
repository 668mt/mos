package mt.spring.mos.server.service.fragment;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.redis.core.RedisTemplate;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Martin
 * @date 2020/5/29
 */
@Slf4j
public class RedisTaskFragment implements TaskFragment {
	
	@Setter
	private String scheduleName = "RedisTaskFragment";
	private final RedisTemplate<String, Object> redisTemplate;
	private final String currentInstanceId;
	private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
	
	
	public static String getHostIp(@Nullable String prefix) {
		try {
			Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
			while (allNetInterfaces.hasMoreElements()) {
				NetworkInterface netInterface = allNetInterfaces.nextElement();
				Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress ip = addresses.nextElement();
					if (ip instanceof Inet4Address
							&& !ip.isLoopbackAddress() //loopback地址即本机地址，IPv4的loopback范围是127.0.0.0 ~ 127.255.255.255
							&& !ip.getHostAddress().contains(":")) {
						String hostAddress = ip.getHostAddress();
						if (StringUtils.isBlank(prefix) || hostAddress.startsWith(prefix)) {
							return hostAddress;
						}
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		throw new IllegalStateException("获取主机ip失败");
	}
	
	public RedisTaskFragment(RedisTemplate<String, Object> redisTemplate) {
		this(redisTemplate, getHostIp(null));
	}
	
	public RedisTaskFragment(RedisTemplate<String, Object> redisTemplate, String currentInstanceId) {
		this.currentInstanceId = currentInstanceId;
		this.redisTemplate = redisTemplate;
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
	
	public String getKey() {
		return "TaskRedisFragment:" + scheduleName;
	}
	
	public String getInstanceKey(String instanceId) {
		return getKey() + ":" + instanceId;
	}
	
	private void startRegist() {
		scheduledThreadPoolExecutor.scheduleAtFixedRate(this::register, 0, 20, TimeUnit.SECONDS);
	}
	
	private void register() {
		redisTemplate.opsForZSet().add(getKey(), currentInstanceId, 100);
		redisTemplate.opsForValue().set(getInstanceKey(currentInstanceId), System.currentTimeMillis(), 30, TimeUnit.SECONDS);
	}
	
	private boolean isItemExpired(String instanceId) {
		Long time = (Long) redisTemplate.opsForValue().get(getInstanceKey(instanceId));
		if (time != null) {
			return System.currentTimeMillis() - time > 30 * 1000;
		}
		return true;
	}
	
	private void deleteExpires() {
		list().stream()
				.map(o -> o + "")
				.filter(this::isItemExpired)
				.forEach(s -> redisTemplate.opsForZSet().remove(getKey(), s));
	}
	
	private long count() {
		Long size = redisTemplate.opsForZSet().size(getKey());
		return size == null ? 0 : size;
	}
	
	private List<String> list() {
		Set<Object> range = redisTemplate.opsForZSet().range(getKey(), 0, count());
		if (range == null) {
			return new ArrayList<>();
		}
		return range.stream()
				.map(Object::toString)
				.collect(Collectors.toList());
	}
	
	private Long index() {
		return redisTemplate.opsForZSet().rank(getKey(), currentInstanceId);
	}
	
	@Override
	public FragmentInfo getCurrentFragmentInfo() {
		deleteExpires();
		FragmentInfo fragmentInfo = new FragmentInfo();
		fragmentInfo.setTotal(count());
		Long index = index();
		if (index == null) {
			throw new IllegalStateException("current instant not register yet, current is:" + list());
		}
		fragmentInfo.setIndex(index);
		return fragmentInfo;
	}
	
	@Override
	public <T> void fragment(Collection<T> tasks, FragmentIdFunction<T> fragmentIdFunction, FragmentJob<T> fragmentJob) {
		fragment(tasks, fragmentIdFunction, fragmentJob, (task, e) -> log.error(e.getMessage(), e));
	}
	
	@Override
	public <T> void fragment(Collection<T> tasks, FragmentIdFunction<T> fragmentIdFunction, FragmentJob<T> fragmentJob, ExceptionHandler<T> exceptionHandler) {
		if (CollectionUtils.isEmpty(tasks)) {
			return;
		}
		waitUntilReady();
		FragmentInfo fragmentInfo = getCurrentFragmentInfo();
		long index = fragmentInfo.getIndex();
		long count = fragmentInfo.getTotal();
		for (T task : tasks) {
			long fragmentId = fragmentIdFunction.getFragmentId(task);
			if (fragmentId < 0) {
				fragmentId = -1 * fragmentId;
			}
			if (fragmentId % count == index) {
				try {
					fragmentJob.doJob(task);
				} catch (Exception e) {
					exceptionHandler.handleException(task, e);
				}
			}
		}
	}
	
	@Override
	public <T> boolean isCurrentJob(T task, FragmentIdFunction<T> fragmentIdFunction) {
		FragmentInfo fragmentInfo = getCurrentFragmentInfo();
		long index = fragmentInfo.getIndex();
		long count = fragmentInfo.getTotal();
		long fragmentId = fragmentIdFunction.getFragmentId(task);
		return fragmentId % count == index;
	}
	
}
