package mt.spring.mos.server.service.resource.render.template;

import com.github.pagehelper.PageHelper;
import mt.common.tkmapper.Filter;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.sdk.MosSdk;
import mt.spring.mos.server.config.aop.MosContext;
import mt.spring.mos.server.entity.po.AccessControl;
import mt.spring.mos.server.entity.po.Dir;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.entity.vo.GallaryVo;
import mt.spring.mos.server.service.AccessControlService;
import mt.spring.mos.server.service.DirService;
import mt.spring.mos.server.service.ResourceService;
import mt.spring.mos.server.service.resource.render.Content;
import mt.spring.mos.server.utils.UrlEncodeUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2021/2/8
 */
@Component
public class ImageGallaryRender extends AbstractTemplateRender {
	@Autowired
	private DirService dirService;
	@Autowired
	private ResourceService resourceService;
	@Autowired
	private AccessControlService accessControlService;
	
	@Override
	public String getTemplatePath() {
		return "gallary/index";
	}
	
	@Override
	public boolean shouldRend(HttpServletRequest request, Content content) {
		if (!content.isGallary()) {
			return false;
		}
		Dir dir = dirService.findOneByPathAndBucketId(content.getPathname(), content.getBucket().getId(), false);
		Assert.notNull(dir, "资源不存在：" + content.getPathname());
		return true;
	}
	
	@Override
	public void addSuffixPatterns(Set<String> suffixPatterns) {
	}
	
	@Override
	public ModelAndView rend(ModelAndView modelAndView, HttpServletRequest request, HttpServletResponse response, Content content) throws Exception {
		Dir dir = dirService.findOneByPathAndBucketId(content.getPathname(), content.getBucket().getId(), false);
		List<String> suffixs = Arrays.asList(".jpg", ".jpeg", ".bmp", ".gif", ".png");
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("dirId", Filter.Operator.eq, dir.getId()));
		filters.add(new Filter("suffix", Filter.Operator.in, suffixs));
		PageHelper.orderBy("name");
		List<Resource> imgs = resourceService.findByFilters(filters);
		List<GallaryVo> gallaryVos = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(imgs)) {
			Long openId = MosContext.getContext().getOpenId();
			MosSdk mosSdk = null;
			if (openId != null) {
				AccessControl accessControl = accessControlService.findById(openId);
				mosSdk = new MosSdk("", openId, content.getBucket().getBucketName(), accessControl.getSecretKey());
			}
			
			MosSdk finalMosSdk = mosSdk;
			gallaryVos = imgs.stream().map(resource -> {
				GallaryVo gallaryVo = new GallaryVo();
				BeanUtils.copyProperties(resource, gallaryVo);
				String pathname = resourceService.getPathname(resource);
				String url = "/mos/" + content.getBucket().getBucketName() + UrlEncodeUtils.encodePathname(pathname);
				String thumbUrl = url + "?thumb=true";
				if (finalMosSdk != null) {
					String sign = finalMosSdk.getSign(pathname, 2, TimeUnit.HOURS);
					url += "?sign=" + sign;
					thumbUrl += "&sign=" + sign;
				}
				gallaryVo.setUrl(url);
				gallaryVo.setThumbUrl(thumbUrl);
				return gallaryVo;
			}).collect(Collectors.toList());
		}
		modelAndView.addObject("imgs", gallaryVos);
		modelAndView.addObject("title", dir.getName().substring(1));
		modelAndView.setViewName(getTemplatePath());
		return modelAndView;
	}
	
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
}
