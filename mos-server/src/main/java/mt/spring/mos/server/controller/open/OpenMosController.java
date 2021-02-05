package mt.spring.mos.server.controller.open;

import io.swagger.annotations.ApiOperation;
import mt.spring.mos.server.annotation.OpenApi;
import mt.spring.mos.server.config.aop.MosContext;
import mt.spring.mos.server.entity.BucketPerm;
import mt.spring.mos.server.entity.po.Audit;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.service.AuditService;
import mt.spring.mos.server.service.BucketService;
import mt.spring.mos.server.service.ClientService;
import mt.spring.mos.server.service.ResourceService;
import mt.spring.mos.server.service.resource.render.Content;
import mt.spring.mos.server.service.resource.render.ResourceRender;
import mt.utils.common.Assert;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.util.Comparator;
import java.util.List;

/**
 * @Author Martin
 * @Date 2021/1/16
 */
@RestController
@RequestMapping("/mos")
public class OpenMosController implements InitializingBean {
	@Autowired
	private CloseableHttpClient httpClient;
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
	
	@GetMapping("/render/{bucketName}/**")
	@ApiOperation("获取资源")
	@OpenApi(pathnamePrefix = "/mos/render/{bucketName}", perms = BucketPerm.SELECT)
	public ModelAndView mosWithRender(@RequestParam(defaultValue = "false") Boolean thumb,
									  @PathVariable String bucketName,
									  HttpServletRequest request,
									  HttpServletResponse httpServletResponse
	) throws Exception {
		String requestURI = request.getRequestURI();
		String pathname = requestURI.substring(("/mos/render/" + bucketName).length() + 1);
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		pathname = URLDecoder.decode(pathname, "UTF-8");
		return requestResouce(bucketName, pathname, thumb, true, request, httpServletResponse);
	}
	
	@GetMapping("/{bucketName}/**")
	@ApiOperation("获取资源")
	@OpenApi(pathnamePrefix = "/mos/{bucketName}", perms = BucketPerm.SELECT)
	public ModelAndView mos(@RequestParam(defaultValue = "false") Boolean thumb,
							@PathVariable String bucketName,
							@RequestParam(defaultValue = "false") Boolean render,
							HttpServletRequest request,
							HttpServletResponse httpServletResponse
	) throws Exception {
		String requestURI = request.getRequestURI();
		String pathname = requestURI.substring(("/mos/" + bucketName).length() + 1);
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		pathname = URLDecoder.decode(pathname, "UTF-8");
		return requestResouce(bucketName, pathname, thumb, render, request, httpServletResponse);
	}
	
	private ModelAndView requestResouce(String bucketName, String pathname, Boolean thumb, Boolean render, HttpServletRequest request, HttpServletResponse httpServletResponse) throws Exception {
		Bucket bucket = bucketService.findOne("bucketName", bucketName);
		Assert.notNull(bucket, "bucket不存在");
		
		Resource resource = resourceService.findResourceByPathnameAndBucketId(pathname, bucket.getId());
		Assert.notNull(resource, "资源不存在");
		Client client = clientService.findRandomAvalibleClientForVisit(resource, thumb);
		Assert.notNull(client, "无可用的资源服务器");
		String url = resourceService.getDesUrl(client, bucket, resource, thumb);
		if (!thumb) {
			auditService.auditResourceVisits(resource.getId());
		}
		Audit audit = auditService.startAudit(MosContext.getContext(), Audit.Type.READ, Audit.Action.visit, thumb ? "缩略图" : null);
		Content content = new Content(bucket, resource, client, url, audit, render);
		for (ResourceRender resourceRender : renders) {
			if (resourceRender.shouldRend(request, content)) {
				return resourceRender.rend(new ModelAndView(), request, httpServletResponse, content);
			}
		}
		throw new IllegalStateException("没有为" + pathname + "找到合适的渲染器");
	}
}
