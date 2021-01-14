package mt.spring.mos.server.service.resource.render.template;

import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.service.resource.render.Content;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

/**
 * @Author Martin
 * @Date 2020/10/31
 */
@Component
public class TxtRender extends AbstractTemplateRender {
	public static final int TXT_RENDER_ORDER = -20;
	
	@Override
	public void addSuffixPatterns(Set<String> suffixPatterns) {
		suffixPatterns.add("*.txt");
	}
	
	@Override
	public String getTemplatePath() {
		return "txt/index";
	}
	
	@Override
	public ModelAndView rend(ModelAndView modelAndView, HttpServletRequest request, HttpServletResponse response, Content content) throws Exception {
		Resource resource = content.getResource();
		String fileName = resource.getName();
		String title2 = fileName.substring(0, fileName.length() - 4);
		modelAndView.addObject("title2", title2);
		modelAndView.addObject("url", getNoRenderUri(request));
		return super.rend(modelAndView, request, response, content);
	}
	
	@Override
	public int getOrder() {
		return TXT_RENDER_ORDER;
	}
}
