package mt.spring.mos.server.service.resource.render.template;

import mt.spring.mos.server.utils.UrlEncodeUtils;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
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
		suffixPatterns.add("*.m3u8");
	}
	
	@Override
	public int getOrder() {
		return -20;
	}
	
	@Override
	public long getMaxSizeByte() {
		return -1;
	}
	
	@Override
	protected String getNoRenderUri(HttpServletRequest request) {
		String noRenderUri = super.getNoRenderUri(request);
		noRenderUri = UrlEncodeUtils.encodePathname(noRenderUri);
		return noRenderUri;
	}
}
