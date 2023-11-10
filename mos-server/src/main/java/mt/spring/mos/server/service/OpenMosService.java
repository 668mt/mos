package mt.spring.mos.server.service;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.exception.NoAvailableClientBizException;
import mt.spring.mos.server.exception.NoAvailableRenderBizException;
import mt.spring.mos.server.exception.ResourceNotFoundBizException;
import mt.spring.mos.server.service.resource.render.Content;
import mt.spring.mos.server.service.resource.render.ResourceRender;
import mt.utils.common.Assert;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Comparator;
import java.util.List;

/**
 * @Author Martin
 * @Date 2021/2/8
 */
@Service
@Slf4j
public class OpenMosService implements InitializingBean {
	@Autowired
	private ClientService clientService;
	@Autowired
	private List<ResourceRender> renders;
	@Autowired
	private AuditService auditService;
	@Autowired
	private BucketService bucketService;
	@Autowired
	private ResourceService resourceService;
	
	@Override
	public void afterPropertiesSet() {
		renders.sort(Comparator.comparingInt(Ordered::getOrder));
	}
	
	
	public String getPathname(HttpServletRequest request, String prefix) throws UnsupportedEncodingException {
		String requestURI = request.getRequestURI();
		String pathname = requestURI.substring((prefix).length() + 1);
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		return URLDecoder.decode(pathname, "UTF-8");
	}
	
	public ModelAndView requestResource(String bucketName, String pathname, Boolean thumb, Boolean render, Boolean gallary, HttpServletRequest request, HttpServletResponse httpServletResponse) throws Exception {
		Bucket bucket = bucketService.findOne("bucketName", bucketName);
		Assert.notNull(bucket, "bucket不存在:" + bucketName);
		
		Resource resource = null;
		String url = null;
		Client client = null;
		if (!gallary) {
			resource = resourceService.findResourceByPathnameAndBucketId(pathname, bucket.getId(), false);
			if (resource == null) {
				throw new ResourceNotFoundBizException(bucketName + "下不存在资源[" + pathname + "]");
			}
			if (!thumb) {
				//新增访问次数
				auditService.addResourceHits(resource.getId(), 1);
				auditService.readRequestsRecord(bucket.getId(), 1);
			}
			client = clientService.findRandomAvalibleClientForVisit(resource, thumb);
			if (client == null) {
				throw new NoAvailableClientBizException("无可用的资源服务器：" + bucketName + "," + pathname);
			}
			url = resourceService.getDesUrl(client, bucket, resource, thumb);
		}
		Content content = new Content(bucket, resource, pathname, client, url, render);
		content.setGallary(gallary);
		content.setThumb(thumb);
		for (ResourceRender resourceRender : renders) {
//			log.info("资源渲染器：" + resourceRender.getClass().getName());
//			log.info("name:{},is m3u8:{},render:{}", content.getResource().getName(), content.getResource().getName().endsWith(".m3u8"), render);
			if (resourceRender.shouldRend(request, content)) {
//				log.error("使用资源渲染器：" + resourceRender.getClass().getName());
				return resourceRender.rend(new ModelAndView(), request, httpServletResponse, content);
			}
		}
		throw new NoAvailableRenderBizException("没有为" + pathname + "找到合适的渲染器");
	}
}
