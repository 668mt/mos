package mt.spring.mos.server.service.resource.render;

import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * @Author Martin
 * @Date 2020/10/31
 */
@Component
public class TxtRender extends AbstractRender {
	
	@Override
	public void addSuffixs(Set<String> suffixs) {
		suffixs.add(".txt");
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
}
