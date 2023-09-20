package mt.spring.mos.client.config.log;

import lombok.Getter;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @Author Martin
 * @Date 2023/9/15
 */
public class TraceCallable<V> implements Callable<V> {
	@Getter
	private final Callable<V> proxy;
	private final String traceId;
	private final Map<String, String> copyOfContextMap;
	
	public TraceCallable(Callable<V> proxy) {
		this(proxy, TraceContext.getTraceId());
	}
	
	public TraceCallable(Callable<V> proxy, String traceId) {
		if (proxy instanceof TraceCallable) {
			this.proxy = ((TraceCallable<V>) proxy).getProxy();
		} else {
			this.proxy = proxy;
		}
		this.traceId = traceId;
		this.copyOfContextMap = MDC.getCopyOfContextMap();
	}
	
	@Override
	public V call() throws Exception {
		try {
			TraceContext.setTraceId(traceId);
			MDC.setContextMap(copyOfContextMap);
			return proxy.call();
		} finally {
			TraceContext.removeTraceId();
		}
	}
}
