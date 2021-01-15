package mt.spring.mos.server.service.resource.render.template;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * @Author Martin
 * @Date 2021/1/14
 */
@Component
public class VideoRender extends AbstractTemplateRender {
	@Override
	public String getTemplatePath() {
		return "video/index";
	}
	
	@Override
	public void addSuffixPatterns(Set<String> suffixPatterns) {
		suffixPatterns.add("*.mp4");
		suffixPatterns.add("*.flv");
	}
	
	@Override
	public int getOrder() {
		return -20;
	}
	
	@Override
	public long getMaxSizeByte() {
		return -1;
	}
}
