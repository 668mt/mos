package mt.spring.mos.server.config.log;

import org.jetbrains.annotations.NotNull;

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
}
