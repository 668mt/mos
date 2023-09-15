package mt.spring.mos.server.config.log;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * @Author Martin
 * @Date 2023/9/15
 */
public class TraceContext {
	private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
	
	public static void setTraceId(@NotNull String traceId) {
		TRACE_ID.set(traceId);
	}
	
	public static String getTraceId() {
		return TRACE_ID.get();
	}
	
	public static void removeTraceId() {
		TRACE_ID.remove();
	}
	
	public static String create() {
		return UUID.randomUUID().toString().replace("-", "");
	}
	
	public static String getOrCreate() {
		String traceId = getTraceId();
		return StringUtils.isBlank(traceId) ? create() : traceId;
	}
}
