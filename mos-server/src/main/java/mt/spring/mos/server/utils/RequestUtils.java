package mt.spring.mos.server.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @Author Martin
 * @Date 2025/11/9
 */
public class RequestUtils {
	/**
	 * 获取请求域名
	 *
	 * @return
	 */
	public static String getRequestDomain() {
		ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (requestAttributes == null) {
			return null;
		}
		HttpServletRequest request = requestAttributes.getRequest();
		
		String s = request.getRequestURL().toString();
		int i1 = s.indexOf("/", 8);
		return s.substring(0, i1);
	}
}
