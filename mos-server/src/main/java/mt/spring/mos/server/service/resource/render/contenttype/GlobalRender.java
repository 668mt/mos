package mt.spring.mos.server.service.resource.render.contenttype;

import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.service.resource.render.AbstractRender;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * @Author Martin
 * @Date 2020/11/22
 */
@Component
public class GlobalRender extends AbstractRender {
	
	@Override
	public void addSuffixPatterns(Set<String> suffixPatterns) {
		suffixPatterns.add("**");
	}
	
	@Override
	protected String getDefaultContentType(Resource resource) {
		return "application/octet-stream";
	}
	
	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}
	
}
