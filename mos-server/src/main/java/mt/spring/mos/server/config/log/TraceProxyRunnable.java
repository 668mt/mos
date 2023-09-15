package mt.spring.mos.server.config.log;

import lombok.Getter;

/**
 * @Author Martin
 * @Date 2023/9/15
 */
public class TraceProxyRunnable implements Runnable {
	@Getter
	private final Runnable proxy;
	private final String traceId;
	
	public TraceProxyRunnable(Runnable proxy) {
		if (proxy instanceof TraceProxyRunnable) {
			this.proxy = ((TraceProxyRunnable) proxy).getProxy();
		} else {
			this.proxy = proxy;
		}
		this.traceId = TraceContext.getTraceId();
	}
	
	@Override
	public void run() {
		try {
			TraceContext.setTraceId(traceId);
			proxy.run();
		} finally {
			TraceContext.removeTraceId();
		}
	}
}
