package mt.spring.mos.server.service.resource.render.template;

import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.Resource;
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
	protected String getContentAsString(Resource resource, String desUrl) {
		String contentAsString = super.getContentAsString(resource, desUrl);
		return contentAsString.replaceAll("\n", "<br/>");
	}
	
	@Override
	public ModelAndView rend(ModelAndView modelAndView, HttpServletRequest request, HttpServletResponse response, Bucket bucket, Resource resource, Client client, String desUrl) throws Exception {
		String fileName = resource.getFileName();
		String title2 = fileName.substring(0, fileName.length() - 4);
		modelAndView.addObject("title2", title2);
		return super.rend(modelAndView, request, response, bucket, resource, client, desUrl);
	}
	
	@Override
	public int getOrder() {
		return TXT_RENDER_ORDER;
	}
}
