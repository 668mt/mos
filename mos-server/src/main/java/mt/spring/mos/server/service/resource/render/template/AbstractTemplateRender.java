package mt.spring.mos.server.service.resource.render.template;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.server.entity.po.Audit;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.service.AuditService;
import mt.spring.mos.server.service.resource.render.AbstractRender;
import mt.spring.mos.server.service.resource.render.Content;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.DefaultUriBuilderFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author Martin
 * @Date 2020/11/22
 */
@Slf4j
public abstract class AbstractTemplateRender extends AbstractRender {
	@Autowired
	protected AuditService auditService;
	protected final DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory();
	
	public abstract String getTemplatePath();
	
	public long getMaxSizeByte() {
		return 1024 * 1024 * 10;
	}
	
	@Override
	protected String getDefaultContentType(Resource resource) {
		return null;
	}
	
	protected String getNoRenderUri(HttpServletRequest request) {
		String uri = request.getRequestURI() + "?render=false";
		if (StringUtils.isNotBlank(request.getQueryString())) {
			uri += "&" + request.getQueryString();
		}
		return uri;
	}
	
	@Override
	public boolean shouldRend(HttpServletRequest request, Bucket bucket, Resource resource) {
		long maxSizeByte = getMaxSizeByte();
		if (maxSizeByte > 0 && resource.getSizeByte() > maxSizeByte) {
			return false;
		}
		return super.shouldRend(request, bucket, resource);
	}
	
	@Override
	public ModelAndView rend(ModelAndView modelAndView, HttpServletRequest request, HttpServletResponse response, Content renderContent) throws Exception {
		Resource resource = renderContent.getResource();
		Audit audit = renderContent.getAudit();
		modelAndView.addObject("url", getNoRenderUri(request));
		modelAndView.addObject("title", resource.getName());
		modelAndView.setViewName(getTemplatePath());
		auditService.endAudit(audit, 0);
		return modelAndView;
	}
}
