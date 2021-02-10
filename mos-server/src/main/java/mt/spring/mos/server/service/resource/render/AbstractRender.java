package mt.spring.mos.server.service.resource.render;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.service.AuditService;
import mt.spring.mos.server.service.CacheControlService;
import mt.spring.mos.server.utils.HttpClientServletUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @Author Martin
 * @Date 2020/10/31
 */
@Slf4j
public abstract class AbstractRender implements ResourceRender {
	@Autowired
	protected CloseableHttpClient httpClient;
	@Autowired
	protected AuditService auditService;
	@Autowired
	private CacheControlService cacheControlService;
	
	protected final AntPathMatcher antPathMatcher = new AntPathMatcher("/");
	
	public abstract void addSuffixPatterns(Set<String> suffixPatterns);
	
	@Override
	public String getContentType(Resource resource) {
		if (StringUtils.isNotBlank(resource.getContentType())) {
			return resource.getContentType();
		}
		return getDefaultContentType(resource);
	}
	
	protected abstract String getDefaultContentType(Resource resource);
	
	
	@Override
	public boolean shouldRend(HttpServletRequest request, Content content) {
		Resource resource = content.getResource();
		if (resource == null) {
			return false;
		}
		Set<String> suffixs = new HashSet<>();
		addSuffixPatterns(suffixs);
		String fileName = resource.getName();
		
		for (String suffix : suffixs) {
			if (antPathMatcher.match(suffix, fileName) || antPathMatcher.match(suffix.toUpperCase(), fileName)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public ModelAndView rend(ModelAndView modelAndView, HttpServletRequest request, HttpServletResponse response, Content content) throws Exception {
		Map<String, String> requestHeaders = new HashMap<>();
		Map<String, String> responseHeaders = new HashMap<>();
		Resource resource = content.getResource();
		String desUrl = content.getDesUrl();
		responseHeaders.put("content-type", getContentType(resource));
		if (cacheControlService.needNoCache(resource.getId(), content.isThumb())) {
			requestHeaders.put("if-modified-since", "-1");
			cacheControlService.clearNoCache(resource.getId(), content.isThumb());
		}
		HttpClientServletUtils.forward(httpClient, desUrl, request, response, auditService.createAuditStream(response.getOutputStream(), content.getAudit()), requestHeaders, responseHeaders);
		return null;
	}
}
