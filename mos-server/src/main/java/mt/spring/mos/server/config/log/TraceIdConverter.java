package mt.spring.mos.server.config.log;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import mt.utils.common.StringUtils;

/**
 * @Author Martin
 * @Date 2023/9/15
 */
public class TraceIdConverter extends ClassicConverter {
	@Override
	public String convert(ILoggingEvent iLoggingEvent) {
		return StringUtils.nullAsDefault(TraceContext.getTraceId(), "");
	}
}
