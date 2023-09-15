package mt.spring.mos.server.config.log;

import lombok.Getter;

import java.util.concurrent.Callable;

/**
 * @Author Martin
 * @Date 2023/9/15
 */
public class TraceProxyCallable<V> implements Callable<V> {
	@Getter
	private final Callable<V> proxy;
	private final String traceId;
	
	public TraceProxyCallable(Callable<V> proxy) {
		if (proxy instanceof TraceProxyCallable) {
			this.proxy = ((TraceProxyCallable<V>) proxy).getProxy();
		} else {
			this.proxy = proxy;
		}
		this.traceId = TraceContext.getTraceId();
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
