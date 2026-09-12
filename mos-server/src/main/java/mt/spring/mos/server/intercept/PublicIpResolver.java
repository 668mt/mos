package mt.spring.mos.server.intercept;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import mt.utils.http.MyHttp;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 公网 IP 解析器：调用远程端点获取当前服务端的公网 IP，并按 30 秒周期在后台刷新。
 * 失败时返回当前缓存值（首次失败返回 null），不抛异常。
 * <p>
 *
 * @Author Martin
 * @Date 2026/9/12
 */
@Slf4j
public class PublicIpResolver {
	
	/**
	 * 后台刷新周期（秒）
	 */
	private static final long REFRESH_PERIOD_SECONDS = 30L;
	
	/**
	 * 进程内缓存的公网 IP；volatile 保证可见性
	 */
	private static volatile String cachedPublicIp;
	
	/**
	 * 当前使用的查询端点（可由外部覆盖）
	 */
	private static volatile String currentEndpoint;
	
	private static final ScheduledExecutorService SCHEDULER =
		new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("public-ip-resolver"));
	
	static {
		// 应用启动后立即跑一次，之后每 30 秒刷新一次
		SCHEDULER.scheduleWithFixedDelay(() -> {
			if (StringUtils.isBlank(currentEndpoint)) {
				return;
			}
			try {
				String ip = fetchPublicIp(currentEndpoint);
				if (ip != null && !ip.isEmpty()) {
					cachedPublicIp = ip;
				}
			} catch (Exception e) {
				log.warn("后台刷新公网 IP 异常", e);
			}
		}, 0, REFRESH_PERIOD_SECONDS, TimeUnit.SECONDS);
	}
	
	/**
	 * 注入查询端点 URL（由 LanRedirectInterceptor 或运维工具在启动时调用）。
	 * 同时清空缓存以触发立即重新解析。
	 *
	 * @param endpoint 新的端点 URL；为空则使用默认值
	 */
	public static void setEndpoint(String endpoint) {
		synchronized (PublicIpResolver.class) {
			if (endpoint == null || endpoint.isEmpty()) {
				return;
			} else {
				currentEndpoint = endpoint;
			}
			cachedPublicIp = null;
			log.info("PublicIpResolver 端点已更新为：{}", currentEndpoint);
		}
	}
	
	/**
	 * 获取当前正在使用的查询端点 URL。
	 */
	public static String getEndpoint() {
		return currentEndpoint;
	}
	
	/**
	 * 获取当前服务端的公网 IP。直接返回缓存值（后台调度器会持续刷新）。
	 * 首次启动后调度器延迟 0 立即执行一次，多数情况下调用时已有缓存。
	 *
	 * @return 公网 IP；首次后台任务未完成且缓存为空时返回 null
	 */
	public static String getCurrentPublicIp() {
		return cachedPublicIp;
	}
	
	/**
	 * 主动调用一次查询（用于测试或运维强制刷新场景，调用方需自行处理失败降级）。
	 */
	public static String fetchPublicIpNow() {
		try {
			String ip = fetchPublicIp(currentEndpoint);
			if (ip != null && !ip.isEmpty()) {
				cachedPublicIp = ip;
			}
			return ip;
		} catch (Exception e) {
			log.warn("主动获取公网 IP 异常", e);
			return null;
		}
	}
	
	/**
	 * 清除进程内缓存（用于测试场景）
	 */
	public static void clearCache() {
		synchronized (PublicIpResolver.class) {
			cachedPublicIp = null;
		}
	}
	
	/**
	 * 关闭后台调度器（仅在 JVM 关闭时调用，避免泄漏线程）
	 */
	public static void shutdown() {
		SCHEDULER.shutdown();
	}
	
	/**
	 * 调用远程端点解析公网 IP。
	 */
	private static String fetchPublicIp(String endpoint) {
		MyHttp myHttp = new MyHttp(endpoint);
		String result = myHttp.connect();
		if (result == null || result.isEmpty()) {
			log.warn("获取公网 IP 失败：响应为空，endpoint={}", endpoint);
			return null;
		}
		JSONObject jsonObject = JSONObject.parseObject(result);
		String ip = jsonObject == null ? null : jsonObject.getString("ip");
		if (ip == null || ip.isEmpty()) {
			log.warn("获取公网 IP 失败：响应中无 ip 字段，endpoint={}，响应内容={}", endpoint, result);
			return null;
		}
		log.debug("获取当前公网 IP 成功：{} (from {})", ip, endpoint);
		return ip;
	}
	
	/**
	 * 自定义线程工厂：为后台刷新线程指定有意义的名称，便于排查。
	 */
	private static class NamedThreadFactory implements ThreadFactory {
		private final String namePrefix;
		private final AtomicInteger counter = new AtomicInteger(0);
		
		NamedThreadFactory(String namePrefix) {
			this.namePrefix = namePrefix;
		}
		
		@Override
		public Thread newThread(@NotNull Runnable r) {
			Thread t = new Thread(r, namePrefix + "-" + counter.incrementAndGet());
			t.setDaemon(true);
			return t;
		}
	}
}
