package mt.spring.mos.server.service.resource.render;

import mt.spring.mos.server.entity.po.Resource;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author Martin
 * @Date 2020/10/31
 */
public interface ResourceRender extends Ordered {
	/**
	 * 是否需要渲染
	 *
	 * @param request
	 * @param content
	 * @return
	 */
	boolean shouldRend(HttpServletRequest request, Content content);
	
	/**
	 * 执行渲染
	 *
	 * @param request
	 * @param response
	 * @param content
	 * @throws Exception
	 */
	ModelAndView rend(ModelAndView modelAndView, HttpServletRequest request, HttpServletResponse response, Content content) throws Exception;
	
	String getContentType(Resource resource);
	
}
