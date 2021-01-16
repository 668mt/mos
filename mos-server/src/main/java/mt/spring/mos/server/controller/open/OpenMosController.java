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
import mt.spring.mos.server.utils.HttpClientServletUtils;
import mt.utils.common.Assert;
import org.apache.commons.lang3.StringUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
	@GetMapping("/{bucketName}/**")
	@ApiOperation("获取资源")
	@OpenApi(pathnamePrefix = "/mos/{bucketName}", perms = BucketPerm.SELECT)
	public ModelAndView mos(@RequestParam(defaultValue = "false") Boolean thumb,
							@RequestParam(defaultValue = "false") Boolean download,
							@RequestParam(defaultValue = "true") Boolean render,
							@PathVariable String bucketName,
							HttpServletRequest request,
							HttpServletResponse httpServletResponse
	) throws Exception {
		String requestURI = request.getRequestURI();
		String pathname = requestURI.substring(("/mos/" + bucketName).length() + 1);
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		String originPathname = URLDecoder.decode(pathname, "UTF-8");
		Bucket bucket = bucketService.findOne("bucketName", bucketName);
		Assert.notNull(bucket, "bucket不存在");
		
		Resource resource = resourceService.findResourceByPathnameAndBucketId(originPathname, bucket.getId());
		Client client = clientService.findRandomAvalibleClientForVisit(resource, thumb);
		Assert.notNull(resource, "资源不存在");
		Assert.notNull(client, "资源不存在");
		String url = resourceService.getDesUrl(client, bucket, resource, thumb);
		String responseContentType = resource.getContentType();
		if (thumb) {
			//缩略图不走渲染
			render = false;
			responseContentType = "image/jpeg";
		} else {
			auditService.auditResourceVisits(resource.getId());
		}
		if (download) {
			responseContentType = "application/octet-stream";
			render = false;
		}
		Audit audit = auditService.startAudit(MosContext.getContext(), Audit.Type.READ, Audit.Action.visit, thumb ? "缩略图" : null);
		if (render) {
			for (ResourceRender resourceRender : renders) {
				if (resourceRender.shouldRend(request, bucket, resource)) {
					return resourceRender.rend(new ModelAndView(), request, httpServletResponse, new Content(bucket, resource, client, url, audit));
				}
			}
		}
		
		if (StringUtils.isBlank(responseContentType)) {
			responseContentType = "application/octet-stream";
		}
		Map<String, String> headers = new HashMap<>();
		headers.put("content-type", responseContentType);
		HttpClientServletUtils.forward(httpClient, url, request, httpServletResponse, auditService.createAuditStream(httpServletResponse.getOutputStream(), audit), headers);
		return null;
	}
}
