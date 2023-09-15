package mt.spring.mos.server.config.log;

import lombok.Getter;

import java.util.concurrent.Callable;

/**
 * @Author Martin
 * @Date 2023/9/15
 */
public class TraceCallable<V> implements Callable<V> {
	@Getter
	private final Callable<V> proxy;
	private final String traceId;
	
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
	}
	
	@Override
	public V call() throws Exception {
		try {
			TraceContext.setTraceId(traceId);
			return proxy.call();
		} finally {
			TraceContext.removeTraceId();
		}
	}
}
