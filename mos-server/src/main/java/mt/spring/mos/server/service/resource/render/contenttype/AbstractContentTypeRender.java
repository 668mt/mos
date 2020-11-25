package mt.spring.mos.server.service.resource.render.contenttype;

import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.service.resource.render.AbstractRender;
import mt.spring.mos.server.utils.HttpClientServletUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author Martin
 * @Date 2020/11/22
 */
public abstract class AbstractContentTypeRender extends AbstractRender {
	@Override
	public void rend(HttpServletRequest request, HttpServletResponse response, Bucket bucket, Resource resource, Client client, String desUrl, String contentType) throws Exception {
		HttpClientServletUtils.forward(httpClient, desUrl, request, response, contentType);
	}
}
