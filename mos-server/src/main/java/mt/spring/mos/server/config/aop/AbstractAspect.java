package mt.spring.mos.server.config.aop;

import org.apache.commons.beanutils.ConvertUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @Author Martin
 * @Date 2020/12/9
 */
public class AbstractAspect {
	@SuppressWarnings("unchecked")
	protected <T> T getParameter(String name, Object[] args, Parameter[] parameters, HttpServletRequest request, Class<T> type) {
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i].getName().equals(name)) {
				return (T) ConvertUtils.convert(args[i], type);
			}
		}
		return (T) ConvertUtils.convert(request.getParameter(name), type);
	}
	
	public ServletRequestAttributes getRequestContext() {
		return (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
	}
	
	protected Method getMethod(JoinPoint joinPoint) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		return signature.getMethod();
	}
	
	public void throwNoPermException(HttpServletResponse response) {
		response.setStatus(HttpStatus.FORBIDDEN.value());
		throw new IllegalStateException("没有权限访问");
	}
}
