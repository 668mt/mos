package mt.spring.mos.server.service.resource.render;

import lombok.Data;
import mt.spring.mos.server.entity.po.Audit;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.Resource;

/**
 * @Author Martin
 * @Date 2020/12/19
 */
@Data
public class Content {
	private Bucket bucket;
	private Resource resource;
	private String pathname;
	private Client client;
	private String desUrl;
	private Audit audit;
	private Boolean render;
	private boolean gallary;
	private boolean thumb;
	
	public Boolean getRender() {
		return render == null ? false : render;
	}
	
	public Content(Bucket bucket, Resource resource, String pathname, Client client, String desUrl, Audit audit, Boolean render) {
		this.bucket = bucket;
		this.resource = resource;
		this.pathname = pathname;
		this.client = client;
		this.desUrl = desUrl;
		this.audit = audit;
		this.render = render;
	}
}
