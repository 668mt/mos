package mt.common.currentUser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CurrentUserHandlerInterceptor extends HandlerInterceptorAdapter {
	@Autowired
	private UserContext userContext;
	/**
	 * "当前用户"属性名称
	 */
	private String currentUserAttributeName = "currentUser";
	
	/**
	 * 请求后处理
	 *
	 * @param request      HttpServletRequest
	 * @param response     HttpServletResponse
	 * @param handler      处理器
	 * @param modelAndView 数据视图
	 */
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		request.setAttribute(currentUserAttributeName, userContext.getCurrentUser());
	}
	
}
