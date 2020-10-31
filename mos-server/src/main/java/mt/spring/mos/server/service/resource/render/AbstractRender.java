package mt.spring.mos.server.service.resource.render;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.utils.UrlEncodeUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.MediaType;

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
	public abstract void addSuffixs(Set<String> suffixs);
	
	public abstract String getTemplatePath();
	
	public long getMaxSizeByte() {
		return 1024 * 1024 * 10;
	}
	
	public void addParams(Map<String, String> params, Bucket bucket, Resource resource, Client client, String desUrl) {
	
	}
	
	@Override
	public boolean shouldRend(HttpServletRequest request, Bucket bucket, Resource resource) {
		String render = request.getParameter("render");
		if ("false".equalsIgnoreCase(render)) {
			return false;
		}
		Set<String> suffixs = new HashSet<>();
		addSuffixs(suffixs);
		String fileName = resource.getFileName();
		if ((getMaxSizeByte() > 0) && resource.getSizeByte() > getMaxSizeByte()) {
			return false;
		}
		
		for (String suffix : suffixs) {
			if (fileName.endsWith(suffix) || fileName.endsWith(suffix.toUpperCase())) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void rend(HttpServletRequest request, HttpServletResponse response, Bucket bucket, Resource resource, Client client, String desUrl) throws Exception {
		Map<String, String> params = new HashMap<>();
		addParams(params, bucket, resource, client, desUrl);
		if (StringUtils.isNotBlank(resource.getContentType())) {
			try {
				MediaType mediaType = MediaType.parseMediaType(resource.getContentType());
				Charset charset = mediaType.getCharset();
				if (charset != null) {
					params.put("charset", charset.toString());
				}
			} catch (Exception e) {
				log.error("解析响应头失败：" + e.getMessage(), e);
			}
		}
		StringBuilder url = new StringBuilder("/render/show?templatePath=" + getTemplatePath() + "&base64Url=" + UrlEncodeUtils.base64Encode(desUrl) + "&title=" + resource.getFileName());
		if (params.size() > 0) {
			for (Map.Entry<String, String> stringStringEntry : params.entrySet()) {
				url.append("&").append(stringStringEntry.getKey()).append("=").append(UrlEncodeUtils.encode(stringStringEntry.getValue()));
			}
		}
		request.getRequestDispatcher(url.toString()).forward(request, response);
	}
}
