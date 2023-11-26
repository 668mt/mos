package mt.spring.mos.server.service.resource.render;

import mt.spring.mos.server.config.aop.MosContext;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.service.AccessControlService;
import mt.spring.mos.server.service.DirService;
import mt.spring.mos.server.service.ResourceService;
import mt.utils.common.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author Martin
 * @Date 2021/2/2
 */
@Component
public class M3u8Render implements ResourceRender {
	@Autowired
	@Qualifier("httpRestTemplate")
	private RestTemplate httpRestTemplate;
	@Autowired
	private ResourceService resourceService;
	@Autowired
	private DirService dirService;
	@Autowired
	private AccessControlService accessControlService;
	
	@Override
	public boolean shouldRend(HttpServletRequest request, Content content) {
		Resource resource = content.getResource();
		if (content.getRender() || !resource.getName().trim().toLowerCase().endsWith(".m3u8")) {
			return false;
		}
		MosContext context = MosContext.getContext();
		return context.getOpenId() != null;
	}
	
	@Override
	public ModelAndView rend(ModelAndView modelAndView, HttpServletRequest request, HttpServletResponse response, Content content) throws Exception {
		String desUrl = content.getDesUrl();
		String m3u8 = httpRestTemplate.getForObject(desUrl, String.class);
		Assert.notNull(m3u8, "资源访问错误：" + desUrl);
		String pathname = resourceService.getPathname(content.getResource());
		String parentPath = dirService.getParentPath(pathname);
		MosContext context = MosContext.getContext();
		Long openId = context.getOpenId();
		accessControlService.useMosSdk(openId, content.getBucket().getBucketName(), mosSdk -> {
			String collect = Stream.of(m3u8.split("\n"))
				.map(s -> {
					if (!s.startsWith("#") && s.endsWith(".ts")) {
						String tsPathname = s;
						if (!tsPathname.startsWith("/")) {
							tsPathname = "/" + tsPathname;
						}
						tsPathname = parentPath + tsPathname;
						String sign = mosSdk.getSign(tsPathname, context.getExpireSeconds(), TimeUnit.SECONDS);
						return s + "?sign=" + sign;
					} else {
						return s;
					}
				}).collect(Collectors.joining("\n"));
			String contentType = getContentType(content.getResource());
			response.setContentType(contentType);
			response.getWriter().write(collect);
			return null;
		});
		return null;
	}
	
	@Override
	public String getContentType(Resource resource) {
//		httpServletResponse.setContentType("application/vnd.apple.mpegURL");
		return "application/vnd.apple.mpegURL";
	}
	
	@Override
	public int getOrder() {
		return -20;
	}
}
