package mt.spring.mos.server.service.resource.render.contenttype;

import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.service.resource.render.AbstractRender;
import mt.spring.mos.server.service.resource.render.Content;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;

/**
 * @Author Martin
 * @Date 2021/1/17
 */
@Component
public class ThumbRender extends AbstractRender {
	
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
	
	@Override
	public boolean shouldRend(HttpServletRequest request, Content content) {
		String thumb = request.getParameter("thumb");
		return Boolean.parseBoolean(thumb);
	}
	
	@Override
	public void addSuffixPatterns(Set<String> suffixPatterns) {
	
	}
	
	@Override
	public String getContentType(Resource resource) {
		return "image/jpeg";
	}
	
	@Override
	protected String getDefaultContentType(Resource resource) {
		return null;
	}
}
