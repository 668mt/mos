package mt.spring.mos.client.config.log;

import lombok.Getter;
import org.slf4j.MDC;

import java.util.Map;

/**
 * @Author Martin
 * @Date 2023/9/15
 */
public class TraceRunnable implements Runnable {
	@Getter
	private final Runnable proxy;
	private final String traceId;
	private final Map<String, String> copyOfContextMap;
	
	public TraceRunnable(Runnable proxy) {
		this(proxy, TraceContext.getTraceId());
	}
	
	public TraceRunnable(Runnable proxy, String traceId) {
		if (proxy instanceof TraceRunnable) {
			this.proxy = ((TraceRunnable) proxy).getProxy();
		} else {
			this.proxy = proxy;
		}
		this.traceId = traceId;
		this.copyOfContextMap = MDC.getCopyOfContextMap();
	}
	
	@Override
	public void run() {
		try {
			TraceContext.setTraceId(traceId);
			MDC.setContextMap(copyOfContextMap);
			proxy.run();
		} finally {
			TraceContext.removeTraceId();
		}
	}
}
