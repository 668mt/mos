package mt.spring.mos.server.service.resource.render;

import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class Content {
	private Bucket bucket;
	private Resource resource;
	private Client client;
	private String desUrl;
	private Audit audit;
	private Boolean render;
}
