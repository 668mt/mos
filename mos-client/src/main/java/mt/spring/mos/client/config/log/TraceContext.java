package mt.spring.mos.client.config.log;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * @Author Martin
 * @Date 2023/9/15
 */
public class TraceContext {
//	private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
	
	public static void setTraceId(@Nullable String traceId) {
		if (StringUtils.isNotBlank(traceId)) {
			MDC.put("traceId", traceId);
//			TRACE_ID.set(traceId);
		}
	}
	
	public static String getTraceId() {
		return MDC.get("traceId");
//		return TRACE_ID.get();
	}
	
	public static void removeTraceId() {
//		TRACE_ID.remove();
		MDC.remove("traceId");
	}
	
	public static String create() {
		return UUID.randomUUID().toString().replace("-", "");
	}
	
	public static String getOrCreate() {
		String traceId = getTraceId();
		return StringUtils.isBlank(traceId) ? create() : traceId;
	}
}
