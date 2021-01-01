package mt.spring.mos.server.service.resource.render.contenttype;

import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.service.resource.render.AbstractRender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author Martin
 * @Date 2020/11/24
 */
@Component
public class ConfigContentTypeRender extends AbstractRender {
	public static final int CONFIG_CONTENT_TYPE_RENDER_ORDER = Ordered.HIGHEST_PRECEDENCE;
	@Autowired
	private MosServerProperties mosServerProperties;
	
	@Override
	public void addSuffixPatterns(Set<String> suffixPatterns) {
		Map<String, MosServerProperties.ContentTypeRender> defaultContentTypes = mosServerProperties.getDefaultContentTypes();
		if (defaultContentTypes != null) {
			for (Map.Entry<String, MosServerProperties.ContentTypeRender> stringContentTypeRenderEntry : defaultContentTypes.entrySet()) {
				suffixPatterns.addAll(stringContentTypeRenderEntry.getValue().getPatterns());
			}
		}
	}
	
	@Override
	protected String getDefaultContentType(Resource resource) {
		for (Map.Entry<String, MosServerProperties.ContentTypeRender> stringContentTypeRenderEntry : mosServerProperties.getDefaultContentTypes().entrySet()) {
			MosServerProperties.ContentTypeRender contentTypeRender = stringContentTypeRenderEntry.getValue();
			List<String> patterns = contentTypeRender.getPatterns();
			for (String pattern : patterns) {
				if (antPathMatcher.match(pattern, resource.getName())) {
					return contentTypeRender.getValue();
				}
			}
		}
		return null;
	}
	
	@Override
	public int getOrder() {
		return CONFIG_CONTENT_TYPE_RENDER_ORDER;
	}
}
