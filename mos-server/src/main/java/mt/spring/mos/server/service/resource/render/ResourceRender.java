package mt.spring.mos.server.service.resource.render;

import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.Resource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author Martin
 * @Date 2020/10/31
 */
public interface ResourceRender {
	/**
	 * 是否需要渲染
	 *
	 * @param request
	 * @param bucket
	 * @param resource
	 * @return
	 */
	boolean shouldRend(HttpServletRequest request, Bucket bucket, Resource resource);
	
	/**
	 * 执行渲染
	 *
	 * @param request
	 * @param response
	 * @param bucket
	 * @param resource
	 * @param client
	 * @param desUrl
	 * @throws Exception
	 */
	void rend(HttpServletRequest request, HttpServletResponse response, Bucket bucket, Resource resource, Client client, String desUrl) throws Exception;
}
