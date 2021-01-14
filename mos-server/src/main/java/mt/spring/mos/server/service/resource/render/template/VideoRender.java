package mt.spring.mos.server.service.resource.render.template;

import mt.spring.mos.server.entity.po.Audit;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.service.resource.render.Content;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

/**
 * @Author Martin
 * @Date 2021/1/14
 */
@Component
public class VideoRender extends AbstractTemplateRender {
	@Override
	public String getTemplatePath() {
		return "video/index";
	}
	
	@Override
	public void addSuffixPatterns(Set<String> suffixPatterns) {
		suffixPatterns.add("*.mp4");
		suffixPatterns.add("*.flv");
	}
	
	@Override
	public int getOrder() {
		return -20;
	}
	
	@Override
	public long getMaxSizeByte() {
		return -1;
	}
	
	@Override
	public ModelAndView rend(ModelAndView modelAndView, HttpServletRequest request, HttpServletResponse response, Content renderContent) throws Exception {
		Resource resource = renderContent.getResource();
		Audit audit = renderContent.getAudit();
		modelAndView.addObject("title", resource.getName());
		modelAndView.addObject("url", getNoRenderUri(request));
		modelAndView.setViewName(getTemplatePath());
		auditService.endAudit(audit, 0);
		return modelAndView;
	}
}
