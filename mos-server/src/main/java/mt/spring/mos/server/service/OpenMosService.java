package mt.spring.mos.server.service;

import mt.spring.mos.server.config.aop.MosContext;
import mt.spring.mos.server.entity.po.Audit;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.Resource;
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
	
	public ModelAndView requestResouce(String bucketName, String pathname, Boolean thumb, Boolean render, Boolean gallary, HttpServletRequest request, HttpServletResponse httpServletResponse) throws Exception {
		Bucket bucket = bucketService.findOne("bucketName", bucketName);
		Assert.notNull(bucket, "bucket不存在");
		
		Resource resource = null;
		String url = null;
		Client client = null;
		if (!gallary) {
			resource = resourceService.findResourceByPathnameAndBucketId(pathname, bucket.getId());
			Assert.notNull(resource, "资源不存在");
			client = clientService.findRandomAvalibleClientForVisit(resource, thumb);
			Assert.notNull(client, "无可用的资源服务器");
			url = resourceService.getDesUrl(client, bucket, resource, thumb);
			if (!thumb) {
				auditService.auditResourceVisits(resource.getId());
			}
		}
		Assert.notNull(resource, "资源不存在：" + pathname);
		Audit audit = auditService.startAudit(MosContext.getContext(), Audit.Type.READ, Audit.Action.visit, thumb ? "缩略图" : null);
		Content content = new Content(bucket, resource, pathname, client, url, audit, render);
		content.setGallary(gallary);
		content.setThumb(thumb);
		for (ResourceRender resourceRender : renders) {
			if (resourceRender.shouldRend(request, content)) {
				return resourceRender.rend(new ModelAndView(), request, httpServletResponse, content);
			}
		}
		throw new IllegalStateException("没有为" + pathname + "找到合适的渲染器");
	}
}
