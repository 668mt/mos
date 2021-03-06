package mt.spring.mos.server.config.aop;

import lombok.extern.slf4j.Slf4j;
import mt.utils.ReflectUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @Author Martin
 * @Date 2020/12/9
 */
@Slf4j
public class AbstractAspect {
	@SuppressWarnings("unchecked")
	protected <T> T getParameter(String name, Object[] args, Parameter[] parameters, HttpServletRequest request, Class<T> type) {
		Object value = null;
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i].getName().equals(name)) {
				value = args[i];
				break;
			}
		}
		if (value == null) {
			value = request.getParameter(name);
		}
		if (value == null && args != null) {
			for (Object arg : args) {
				if (arg == null) {
					continue;
				}
				Field field = ReflectUtils.findField(arg.getClass(), name);
				if (field != null) {
					try {
						field.setAccessible(true);
						value = field.get(arg);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return (T) ConvertUtils.convert(value, type);
	}
	
	public ServletRequestAttributes getRequestContext() {
		return (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
	}
	
	protected Method getMethod(JoinPoint joinPoint) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		return signature.getMethod();
	}
	
	public void throwNoPermException(HttpServletResponse response, String message) {
		response.setStatus(HttpStatus.FORBIDDEN.value());
		throw new IllegalStateException("没有权限访问：" + message);
	}
}
