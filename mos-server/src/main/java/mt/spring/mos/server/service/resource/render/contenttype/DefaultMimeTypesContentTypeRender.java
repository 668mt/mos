package mt.spring.mos.server.service.resource.render.contenttype;

import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.service.resource.render.AbstractRender;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * @Author Martin
 * @Date 2020/11/24
 */
@Component
public class DefaultMimeTypesContentTypeRender extends AbstractRender {
	public static final int DEFAULT_MIME_CONTENT_TYPE_RENDER_ORDER = Ordered.LOWEST_PRECEDENCE - 1;
	
	@Override
	public void addSuffixPatterns(Set<String> suffixPatterns) {
		MimeMappings aDefault = MimeMappings.DEFAULT;
		aDefault.getAll().forEach(mapping -> {
			suffixPatterns.add("*." + mapping.getExtension());
		});
	}
	
	@Override
	protected String getDefaultContentType(Resource resource) {
		return MimeMappings.DEFAULT.get(resource.getExtension());
	}
	
	@Override
	public int getOrder() {
		return DEFAULT_MIME_CONTENT_TYPE_RENDER_ORDER;
	}
}
