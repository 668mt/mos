package mt.spring.mos.server.intercept;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.MosServerProperties.LanRedirectConfig;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.regex.Pattern;

/**
 * mos 资源接口的内网/公网重定向拦截器。
 * 命中策略时返回 302 短路后续处理，未命中时放行。
 *
 * 判定流程：
 * <ol>
 *     <li>非域名（IP 直连）→ 放行</li>
 *     <li>域名不在白名单 → 放行</li>
 *     <li>域名在白名单 + 同局域网（客户端公网 IP == 服务端公网 IP）→ 302 内网</li>
 *     <li>域名在白名单 + 不同局域网 + 公网 IP 可解析 → 302 公网</li>
 *     <li>域名在白名单 + 不同局域网 + 公网 IP 解析失败 → 放行 + warn</li>
 * </ol>
 *
 * @Author Martin
 * @Date 2026/9/12
 */
@Slf4j
@Component
public class LanRedirectInterceptor implements HandlerInterceptor {

	private static final String SCHEME = "http";

	/**
	 * 防止重定向循环的 marker：重定向 URL 上追加该查询参数，
	 * 拦截器若检测到该 marker 则放行，避免对跟随 302 的请求再次重定向。
	 */
	private static final String LOOP_GUARD_PARAM = "from_lan_redirect";
	private static final String LOOP_GUARD_VALUE = "1";

	/**
	 * IPv4 字面量正则（用于识别 IP 直连访问）
	 */
	private static final Pattern IPV4_PATTERN = Pattern.compile(
		"^(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)"
			+ "(\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)){3}$");

	/**
	 * IPv6 字面量（包含 [] / :: / 十六进制段）粗略匹配
	 */
	private static final Pattern IPV6_PATTERN = Pattern.compile(
		"^([0-9a-fA-F]{1,4}:){2,7}[0-9a-fA-F]{1,4}$|^::1$|^::$");

	@Autowired
	private MosServerProperties mosServerProperties;

	/**
	 * 内网重定向端口：复用 server.port
	 */
	@Value("${server.port}")
	private Integer serverPort;

	/**
	 * 应用启动后注入公网 IP 查询端点 URL 到 PublicIpResolver。
	 * 通过 ContextRefreshedEvent 触发，确保配置已绑定完成。
	 */
	@EventListener(ContextRefreshedEvent.class)
	public void onContextRefreshed() {
		LanRedirectConfig config = mosServerProperties.getLanRedirect();
		if (config == null) {
			return;
		}
		String endpoint = config.resolvePublicIpEndpoint();
		PublicIpResolver.setEndpoint(endpoint);
		PublicIpResolver.fetchPublicIpNow();
	}

	@Override
	public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
		LanRedirectConfig config = mosServerProperties.getLanRedirect();
		if (config == null) {
			return true;
		}

		// 0. 总开关：未启用时一律放行，避免对现有用户产生影响
		if (!config.isEnabled()) {
			return true;
		}

		List<String> domains = config.getDomains();
		if (domains == null || domains.isEmpty()) {
			return true;
		}

		String serverName = request.getServerName();
		if (serverName == null || serverName.isEmpty()) {
			return true;
		}

		// 1. IP 直连（request.getServerName 是 IPv4/IPv6 字面量）→ 放行
		if (isIpLiteral(serverName)) {
			return true;
		}

		// 2. 域名不在白名单 → 放行
		if (!domains.contains(serverName)) {
			return true;
		}

		// 2.1 防循环：跟随 302 重定向而来的请求带 marker → 放行，避免无限重定向
		if (hasLoopGuard(request)) {
			return true;
		}

		String clientIp = resolveClientIp(request);
		String serverPublicIp = safeResolvePublicIp(config);

		// 同局域网判定：客户端公网 IP 与服务端公网 IP 字符串相等
		if (clientIp != null && !clientIp.isEmpty()
			&& serverPublicIp != null && !serverPublicIp.isEmpty()
			&& clientIp.equals(serverPublicIp)) {
			// 3. 同局域网 → 302 内网
			String location = buildLocation(mosServerProperties.getCurrentIp(), serverPort,
				request, serverName);
			sendRedirect(response, location, "内网", clientIp);
			return false;
		}

		// 4/5. 不同局域网（任一侧为空/不相等） → 302 公网（公网 IP 解析失败则降级放行）
		if (serverPublicIp == null || serverPublicIp.isEmpty()) {
			log.warn("域名[{}]访问命中白名单但公网 IP 解析失败，降级放行原请求", serverName);
			return true;
		}
		Integer publicPort = config.resolvePublicPort(serverPort);
		String location = buildLocation(serverPublicIp, publicPort, request, serverName);
		sendRedirect(response, location, "公网", clientIp);
		return false;
	}

	/**
	 * 构造重定向 Location：复用 OpenMosService#getPathname 相同的 pathname 截取规则。
	 * 重定向 URL 追加 {@link #LOOP_GUARD_PARAM}={@link #LOOP_GUARD_VALUE} 标记，
	 * 用于拦截器识别"跟随 302 重定向而来的请求"并放行，避免无限循环重定向。
	 */
	private String buildLocation(String ip, Integer port, HttpServletRequest request, String serverName) {
		String requestURI = request.getRequestURI();
		StringBuilder sb = new StringBuilder();
		sb.append(SCHEME).append("://").append(ip).append(":").append(port);
		sb.append(requestURI);
		String queryString = request.getQueryString();
		if (queryString != null && !queryString.isEmpty()) {
			sb.append('?').append(queryString);
		}
		// 追加循环防护 marker
		sb.append(queryString != null && !queryString.isEmpty() ? '&' : '?');
		sb.append(LOOP_GUARD_PARAM).append('=').append(LOOP_GUARD_VALUE);
		return sb.toString();
	}

	/**
	 * 判断当前请求是否为跟随 302 重定向而来的请求。
	 * 通过 query string 中是否携带循环防护 marker 来识别。
	 */
	private boolean hasLoopGuard(HttpServletRequest request) {
		String queryString = request.getQueryString();
		if (queryString == null || queryString.isEmpty()) {
			return false;
		}
		// 精确匹配形如 from_lan_redirect=1，支持以 & 或 ? 起始
		String marker = LOOP_GUARD_PARAM + "=" + LOOP_GUARD_VALUE;
		for (String param : queryString.split("&")) {
			if (marker.equals(param)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 设置 CORS 头并 sendRedirect。
	 */
	private void sendRedirect(HttpServletResponse response, String location, String type, String clientIp) throws java.io.IOException {
		setCorsHeaders(response);
		log.debug("命中{}重定向，clientIp={} → Location={}", type, clientIp, location);
		response.sendRedirect(location);
	}

	/**
	 * 与 OpenMosController#mos 既有 CORS 头保持一致。
	 */
	private void setCorsHeaders(HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Credentials", "true");
		response.setHeader("Access-Control-Expose-Headers", "*");
		response.setHeader("Access-Control-Allow-Headers", "*");
	}

	/**
	 * 安全解析服务端公网 IP：捕获所有异常，避免污染重定向判定主流程。
	 */
	private String safeResolvePublicIp(LanRedirectConfig config) {
		try {
			return config.resolvePublicIp();
		} catch (Exception e) {
			log.warn("解析服务端公网 IP 异常", e);
			return null;
		}
	}

	/**
	 * 判断字符串是否为 IPv4 / IPv6 字面量；用于识别 IP 直连访问。
	 * 使用正则而非 InetAddress.getByName，避免触发 DNS 反查。
	 */
	private boolean isIpLiteral(String value) {
		if (value == null) {
			return false;
		}
		if (value.startsWith("[") && value.endsWith("]")) {
			// IPv6 带方括号形式，例如 [::1]
			return IPV6_PATTERN.matcher(value.substring(1, value.length() - 1)).matches();
		}
		return IPV4_PATTERN.matcher(value).matches() || IPV6_PATTERN.matcher(value).matches();
	}

	/**
	 * 解析客户端真实公网 IP（适配 nginx 反向代理场景）。
	 * 优先读取顺序：X-Forwarded-For 第一段 → X-Real-IP → getRemoteAddr()。
	 *
	 * 注意：依赖 nginx 在反代时透传 X-Forwarded-For / X-Real-IP，
	 * 且本服务仅在内网/受控环境下对外暴露，避免被公网直接访问时伪造 IP 绕过判定。
	 */
	private String resolveClientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (xff != null && !xff.isEmpty()) {
			// 取第一个非空段（最左侧为真实客户端）
			for (String segment : xff.split(",")) {
				String trimmed = segment.trim();
				if (!trimmed.isEmpty()) {
					return trimmed;
				}
			}
		}
		String xRealIp = request.getHeader("X-Real-IP");
		if (xRealIp != null && !xRealIp.isEmpty()) {
			return xRealIp.trim();
		}
		return request.getRemoteAddr();
	}
}
