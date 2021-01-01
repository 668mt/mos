package mt.spring.mos.server.service.resource.render.template;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.server.entity.po.Audit;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.service.AuditService;
import mt.spring.mos.server.service.resource.render.AbstractRender;
import mt.spring.mos.server.service.resource.render.Content;
import mt.utils.http.MyHttp;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.DefaultUriBuilderFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * @Author Martin
 * @Date 2020/11/22
 */
@Slf4j
public abstract class AbstractTemplateRender extends AbstractRender {
	@Autowired
	private AuditService auditService;
	protected final DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory();
	
	public abstract String getTemplatePath();
	
	public long getMaxSizeByte() {
		return 1024 * 1024 * 10;
	}
	
	@Override
	protected String getDefaultContentType(Resource resource) {
		return null;
	}
	
	@Override
	public boolean shouldRend(HttpServletRequest request, Bucket bucket, Resource resource) {
		String render = request.getParameter("render");
		if ("false".equalsIgnoreCase(render)) {
			return false;
		}
		if ((getMaxSizeByte() > 0) && resource.getSizeByte() > getMaxSizeByte()) {
			return false;
		}
		return super.shouldRend(request, bucket, resource);
	}
	
	protected String getContentAsString(Resource resource, String desUrl) {
		String charset = "UTF-8";
		String contentType = getContentType(resource);
		if (StringUtils.isNotBlank(contentType)) {
			try {
				MediaType mediaType = MediaType.parseMediaType(resource.getContentType());
				if (mediaType.getCharset() != null) {
					charset = mediaType.getCharset().toString();
				}
			} catch (Exception e) {
				log.error("解析响应头失败：" + e.getMessage(), e);
			}
		}
		
		MyHttp myHttp = new MyHttp(uriFactory.expand(desUrl).toString());
		if (StringUtils.isBlank(charset)) {
			charset = "UTF-8";
		}
		myHttp.setEncode(charset);
		return myHttp.connect();
	}
	
	@Override
	public ModelAndView rend(ModelAndView modelAndView, HttpServletRequest request, HttpServletResponse response, Content renderContent) throws Exception {
		Resource resource = renderContent.getResource();
		String desUrl = renderContent.getDesUrl();
		Audit audit = renderContent.getAudit();
		String content = getContentAsString(resource, desUrl);
		modelAndView.addObject("content", content);
		modelAndView.addObject("title", resource.getName());
		modelAndView.setViewName(getTemplatePath());
		auditService.endAudit(audit, content.getBytes(StandardCharsets.UTF_8).length);
		return modelAndView;
	}
}
