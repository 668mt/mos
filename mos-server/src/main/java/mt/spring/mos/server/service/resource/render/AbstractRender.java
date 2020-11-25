package mt.spring.mos.server.service.resource.render;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.utils.HttpClientServletUtils;
import mt.spring.mos.server.utils.UrlEncodeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.Charset;
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
	public boolean shouldRend(HttpServletRequest request, Bucket bucket, Resource resource) {
		Set<String> suffixs = new HashSet<>();
		addSuffixPatterns(suffixs);
		String fileName = resource.getFileName();
		
		for (String suffix : suffixs) {
			if (antPathMatcher.match(suffix, fileName) || antPathMatcher.match(suffix.toUpperCase(), fileName)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void rend(HttpServletRequest request, HttpServletResponse response, Bucket bucket, Resource resource, Client client, String desUrl, String contentType) throws Exception {
		HttpClientServletUtils.forward(httpClient, desUrl, request, response, contentType);
	}
}
