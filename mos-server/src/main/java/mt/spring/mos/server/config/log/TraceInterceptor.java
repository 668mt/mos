package mt.spring.mos.server.config.log;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * @Author Martin
 * @Date 2023/9/15
 */
@Component
public class TraceInterceptor implements HandlerInterceptor {
	@Override
	public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
		String traceId = request.getHeader("traceId");
		if (traceId == null) {
			traceId = UUID.randomUUID().toString();
		}
		TraceContext.setTraceId(traceId);
		return true;
	}
	
	@Override
	public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, Exception ex) throws Exception {
		TraceContext.removeTraceId();
	}
}
