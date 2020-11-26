package mt.spring.mos.server.service.resource.render.template;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * @Author Martin
 * @Date 2020/10/31
 */
@Component
public class MarkDownRender extends AbstractTemplateRender {
	public static final int MARKDOWN_RENDER_ORDER = -20;
	
	@Override
	public void addSuffixPatterns(Set<String> suffixPatterns) {
		suffixPatterns.add("*.md");
	}
	
	@Override
	public String getTemplatePath() {
		return "markdown/index";
	}
	
	@Override
	public int getOrder() {
		return MARKDOWN_RENDER_ORDER;
	}
}
