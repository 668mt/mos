package mt.spring.mos.client.config.log;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.apache.commons.lang3.StringUtils;

/**
 * @Author Martin
 * @Date 2023/9/15
 */
public class TraceIdConverter extends ClassicConverter {
	@Override
	public String convert(ILoggingEvent iLoggingEvent) {
		if (StringUtils.isNotBlank(TraceContext.getTraceId())) {
			return TraceContext.getTraceId();
		}
		return "";
	}
}
