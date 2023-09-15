package mt.spring.mos.server.config.log;

import lombok.Getter;

/**
 * @Author Martin
 * @Date 2023/9/15
 */
public class TraceRunnable implements Runnable {
	@Getter
	private final Runnable proxy;
	private final String traceId;
	
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
