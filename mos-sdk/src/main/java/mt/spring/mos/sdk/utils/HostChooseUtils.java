package mt.spring.mos.sdk.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.base.utils.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
		/**
		 * 是否可用
		 */
		private boolean available;
		/**
		 * 内网地址（[] 前面的），健康检查和实际请求都用它
		 */
		private String host;
		/**
		 * 外网地址（[] 里面的），没配 [] 时为 null
		 */
		private String externHost;
		private long parseTime;
		private int weight;
	}
	
	private static final OkHttpClient httpClient = new OkHttpClient.Builder()
			.connectTimeout(5, TimeUnit.SECONDS)
			.readTimeout(5, TimeUnit.SECONDS)
			.callTimeout(5, TimeUnit.SECONDS)
			.build();
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
		//按内网地址缓存（健康检查探测的是内网地址）
		String realHost = parseAddress(host).getHost();
		return StringUtils.isNotBlank(checkUri) ? realHost + checkUri : realHost;
	}
	
	public static ParseResult parseHost(@NotNull String host, @Nullable String checkUri) {
		ParseResult parseResult = parseAddress(host);
		boolean available;
		if (StringUtils.isNotBlank(checkUri)) {
			//健康检查走内网地址
			available = checkAvailable(parseResult.getHost(), checkUri);
		} else {
			available = true;
		}
		parseResult.setAvailable(available);
		parseResult.setParseTime(System.currentTimeMillis());
		return parseResult;
	}
	
	/**
	 * 解析地址，不做健康检查
	 *
	 * @param host 例如：http://localhost:9700[http://rs1.com](70)
	 * @return
	 */
	public static ParseResult parseAddress(@NotNull String host) {
		String realHost;
		String externHost = null;
		int weight = 100;
		//先剥离权重后缀 (N)，再拆外网地址 []
		int index = host.lastIndexOf("(");
		if (index != -1 && host.endsWith(")") && isWeight(host.substring(index + 1, host.length() - 1))) {
			weight = Integer.parseInt(host.substring(index + 1, host.length() - 1));
			realHost = host.substring(0, index).trim();
		} else {
			realHost = host;
		}
		//拆分内网地址[外网地址]
		int leftIndex = realHost.indexOf("[");
		int rightIndex = realHost.lastIndexOf("]");
		if (leftIndex != -1 && rightIndex > leftIndex) {
			externHost = realHost.substring(leftIndex + 1, rightIndex).trim();
			realHost = realHost.substring(0, leftIndex).trim();
		}
		realHost = addProtocol(realHost);
		if (externHost != null) {
			externHost = addProtocol(externHost);
		}
		
		ParseResult parseResult = new ParseResult();
		parseResult.setHost(realHost);
		parseResult.setExternHost(externHost);
		parseResult.setWeight(weight);
		return parseResult;
	}
	
	/**
	 * 补协议头，没写协议时默认 http://
	 *
	 * @param host
	 * @return
	 */
	private static String addProtocol(String host) {
		if (StringUtils.isBlank(host)) {
			return host;
		}
		if (!host.startsWith("http")) {
			return "http://" + host;
		}
		return host;
	}
	
	/**
	 * 判断是否是合法的权重
	 *
	 * @param text
	 * @return
	 */
	private static boolean isWeight(String text) {
		if (StringUtils.isBlank(text)) {
			return false;
		}
		for (int i = 0; i < text.length(); i++) {
			if (!Character.isDigit(text.charAt(i))) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 健康检查
	 *
	 * @param realHost 内网地址
	 * @param checkUri 健康检查地址
	 * @return
	 */
	private static boolean checkAvailable(String realHost, String checkUri) {
		String url = realHost + checkUri;
		try (Response response = httpClient.newCall(new okhttp3.Request.Builder().url(url).get().build()).execute()) {
			return response.isSuccessful();
		} catch (Exception e) {
			log.warn("服务器{}不可用", realHost);
			return false;
		}
	}
	
	/**
	 * 根据权重选择
	 *
	 * @param hosts    url 例如http://192.168.0.174:4100(50)，可带外网地址 http://192.168.0.174:4100[http://rs1.com](50)
	 * @param checkUri 健康检查地址，如果为空则不检查
	 * @return 选中的服务器，没有可用服务器时返回 null
	 */
	public static Host getAvailableHostByWeight(@NotNull List<String> hosts, @Nullable String checkUri) {
		return getAvailableHostByWeight(hosts, 10000L, checkUri);
	}
	
	/**
	 * 获取可用的服务器，被动检查
	 *
	 * @param hosts       url 例如http://192.168.0.174:4100(50)，可带外网地址 http://192.168.0.174:4100[http://rs1.com](50)
	 * @param expiredTime 检查过期时间
	 * @param checkUri    健康检查地址，如果为空则不检查
	 * @return 选中的服务器，没有可用服务器时返回 null
	 */
	public static Host getAvailableHostByWeight(@NotNull List<String> hosts, long expiredTime, @Nullable String checkUri) {
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
			ParseResult selected = list.get(0);
			return new Host(selected.getHost(), selected.getExternHost(), selected.getWeight());
		}
		return null;
	}
	
	
	public static void main(String[] args) {
		List<String> domains = new ArrayList<>();
		domains.add("http://192.168.0.174:4102[http://rs1.com](1)");
		domains.add("http://192.168.0.2:4100[http://rs2.com](0)");
		Host availableHostByWeight = HostChooseUtils.getAvailableHostByWeight(domains, null);
		System.out.println("内网地址: " + availableHostByWeight.getHost());
		System.out.println("外网地址: " + availableHostByWeight.getExternHost());
//		for (int i = 0; i < 100; i++) {
//			String availableHostByWeight = HostChooseUtils.getAvailableHostByWeight(domains, null);
//			System.out.println(availableHostByWeight);
////			Assert.state(availableHostByWeight.contains("174"), "failed");
//		}
	}
}
