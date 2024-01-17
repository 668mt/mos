package mt.spring.mos.sdk.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.base.utils.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

/**
 * @Author Martin
 * @Date 2022/11/10
 */
@Slf4j
public class HostChooseUtils {
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ParseResult {
		private boolean available;
		private String host;
		private long parseTime;
		private int weight;
	}
	
	private static final CloseableHttpClient httpClient = HttpClients.createDefault();
	private static final ExecutorService executorService = new ThreadPoolExecutor(5, 5, 0, TimeUnit.SECONDS, new LinkedBlockingDeque<>(), new NamePrefixThreadFactory("host-choose"));
	private static final Map<String, ParseResult> hostCache = new ConcurrentHashMap<>(16);
	private static final Map<String, ScheduledThreadPoolExecutor> scheduledMap = new ConcurrentHashMap<>(16);
	
	
	public interface HostGetter {
		List<String> getHosts();
	}
	
	public interface CheckCallback {
		void onOk(@Nullable List<String> healthHosts);
	}
	
	/**
	 * 注册主动检查任务
	 *
	 * @param name               任务名
	 * @param hostGetter         获取需要检查的主机
	 * @param checkUri           检查的地址
	 * @param checkIntervalMills 间隔毫秒
	 */
	public static void registerHostCheck(@NotNull String name, @NotNull HostGetter hostGetter, @NotNull String checkUri, long checkIntervalMills) {
		registerHostCheck(name, hostGetter, checkUri, checkIntervalMills, null);
	}
	
	/**
	 * 注册主动检查任务
	 *
	 * @param name               任务名
	 * @param hostGetter         获取需要检查的主机
	 * @param checkUri           检查的地址
	 * @param checkIntervalMills 间隔毫秒
	 * @param checkCallback      检查回调
	 */
	public static void registerHostCheck(@NotNull String name, @NotNull HostGetter hostGetter, @NotNull String checkUri, long checkIntervalMills, @Nullable CheckCallback checkCallback) {
		ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = scheduledMap.get(name);
		Assert.state(scheduledThreadPoolExecutor == null, "已存在检查任务：" + name);
		scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1, new NamePrefixThreadFactory("hostCheck-" + name));
		scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> {
			List<String> healthHosts = new ArrayList<>();
			for (String host : hostGetter.getHosts()) {
				ParseResult parseResult = parseHost(host, checkUri);
				hostCache.put(getCacheKey(host, checkUri), parseResult);
				if (parseResult.isAvailable()) {
					healthHosts.add(host);
				}
			}
			if (checkCallback != null) {
				checkCallback.onOk(healthHosts);
			}
		}, 10000, checkIntervalMills, TimeUnit.MILLISECONDS);
		scheduledMap.put(name, scheduledThreadPoolExecutor);
	}
	
	private static String getCacheKey(@NotNull String host, @Nullable String checkUri) {
		String realHost;
		int index = host.lastIndexOf("(");
		if (index != -1) {
			realHost = host.substring(0, index).trim();
		} else {
			realHost = host;
		}
		return StringUtils.isNotBlank(checkUri) ? realHost + checkUri : realHost;
	}
	
	public static ParseResult parseHost(@NotNull String host, @Nullable String checkUri) {
		if (!host.startsWith("http")) {
			host = "http://" + host;
		}
		String realHost;
		int weight = 100;
		int index = host.lastIndexOf("(");
		if (index != -1) {
			String weightString = host.substring(index + 1);
			realHost = host.substring(0, index).trim();
			weight = Integer.parseInt(weightString.substring(0, weightString.length() - 1));
		} else {
			realHost = host;
		}
		
		boolean available;
		if (StringUtils.isNotBlank(checkUri)) {
			Future<?> future = executorService.submit(() -> {
				String url = realHost + checkUri;
				try (CloseableHttpResponse response = httpClient.execute(new HttpGet(url))) {
					int statusCode = response.getStatusLine().getStatusCode();
					Assert.state(statusCode >= 200 && statusCode < 300, "检查失败：" + statusCode);
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			try {
				future.get(5, TimeUnit.SECONDS);
				available = true;
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				log.warn("服务器{}不可用", realHost);
				available = false;
			}
		} else {
			available = true;
		}
		
		ParseResult parseResult = new ParseResult();
		parseResult.setAvailable(available);
		parseResult.setHost(realHost);
		parseResult.setParseTime(System.currentTimeMillis());
		parseResult.setWeight(weight);
		return parseResult;
	}
	
	/**
	 * 根据权重选择
	 *
	 * @param hosts    url 例如http://192.168.0.174:4100(50)
	 * @param checkUri 健康检查地址，如果为空则不检查
	 * @return
	 */
	public static String getAvailableHostByWeight(@NotNull List<String> hosts, @Nullable String checkUri) {
		return getAvailableHostByWeight(hosts, 10000L, checkUri);
	}
	
	/**
	 * 获取可用的服务器，被动检查
	 *
	 * @param hosts       url 例如http://192.168.0.174:4100(50)
	 * @param expiredTime 检查过期时间
	 * @param checkUri    健康检查地址，如果为空则不检查
	 * @return
	 */
	public static String getAvailableHostByWeight(@NotNull List<String> hosts, long expiredTime, @Nullable String checkUri) {
		List<ParseResult> list = new ArrayList<>();
		for (String host : hosts) {
			if (StringUtils.isNotBlank(checkUri)) {
				String key = getCacheKey(host, checkUri);
				ParseResult parseResult = hostCache.get(key);
				if (parseResult == null || System.currentTimeMillis() > parseResult.getParseTime() + expiredTime) {
					parseResult = parseHost(host, checkUri);
				}
				hostCache.put(key, parseResult);
				if (parseResult.isAvailable()) {
					list.add(parseResult);
				}
			} else {
				list.add(parseHost(host, checkUri));
			}
		}
		if (CollectionUtils.isNotEmpty(list)) {
			Random random = new Random();
			boolean hasWeightGreaterThanZero = list.stream().anyMatch(parseResult -> parseResult.getWeight() > 0);
			if (!hasWeightGreaterThanZero) {
				list.forEach(parseResult -> parseResult.setWeight(100));
			}
			
			list.sort((o1, o2) -> {
				int weight1 = o1.getWeight() > 0 ? random.nextInt(o1.getWeight()) + 1 : 0;
				int weight2 = o2.getWeight() > 0 ? random.nextInt(o2.getWeight()) + 1 : 0;
				//从大到小排序
				return weight2 - weight1;
			});
			return list.get(0).getHost();
		}
		return null;
	}
	
	
	public static void main(String[] args) {
		List<String> domains = new ArrayList<>();
		domains.add("http://192.168.0.174:4102(1)");
		domains.add("http://192.168.0.2:4100(0)");
		String availableHostByWeight = HostChooseUtils.getAvailableHostByWeight(domains, "/index.html");
		System.out.println(availableHostByWeight);
//		for (int i = 0; i < 100; i++) {
//			String availableHostByWeight = HostChooseUtils.getAvailableHostByWeight(domains, null);
//			System.out.println(availableHostByWeight);
////			Assert.state(availableHostByWeight.contains("174"), "failed");
//		}
	}
}
