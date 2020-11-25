package mt.spring.mos.server.service.resource.render.template;

import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.service.resource.render.template.AbstractTemplateRender;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * @Author Martin
 * @Date 2020/10/31
 */
@Component
public class TxtRender extends AbstractTemplateRender {
	
	@Override
	public void addSuffixPatterns(Set<String> suffixPatterns) {
		suffixPatterns.add("*.txt");
	}
	
	@Override
	public String getTemplatePath() {
		return "txt/index";
	}
	
	@Override
	public void addParams(Map<String, String> params, Bucket bucket, Resource resource, Client client, String desUrl) {
		String fileName = resource.getFileName();
		params.put("title2", fileName.substring(0, fileName.length() - 4));
	}
	
	@Override
	public int getOrder() {
		return 0;
	}
}
