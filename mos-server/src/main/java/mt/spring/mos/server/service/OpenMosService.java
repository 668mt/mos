package mt.spring.mos.server.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.exception.NoAvailableClientBizException;
import mt.spring.mos.server.exception.NoAvailableRenderBizException;
import mt.spring.mos.server.exception.ResourceNotFoundBizException;
import mt.spring.mos.server.service.resource.render.Content;
import mt.spring.mos.server.service.resource.render.ResourceRender;
import mt.spring.mos.server.utils.UrlEncodeUtils;
import mt.utils.common.Assert;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
	@Autowired
	private StringRedisTemplate stringRedisTemplate;
	
	private static final String SHORT_URL_KEY_PREFIX = "mos:short:";
	private static final String SHORT_URL_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
	private static final int SHORT_URL_LENGTH = 6;
	private static final int SHORT_URL_MAX_RETRY = 10;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	
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
			if (resourceRender.shouldRend(request, content)) {
				return resourceRender.rend(new ModelAndView(), request, httpServletResponse, content);
			}
		}
		throw new NoAvailableRenderBizException("没有为" + pathname + "找到合适的渲染器");
	}
	
	public String createShortUrl(String bucketName, String pathname, String sign, String queryString, long expireSeconds) {
		Assert.notBlank(bucketName, "bucketName不能为空");
		Assert.notBlank(pathname, "pathname不能为空");
		Assert.notBlank(sign, "sign不能为空");
		Assert.state(pathname.startsWith("/"), "pathname必须以/开头");
		
		pathname = UrlEncodeUtils.encodePathname(pathname);
		Assert.state(!sign.contains("|"), "sign不能包含|");
		Assert.state(expireSeconds > 0, "expireSeconds必须为正整数");
		
		StringBuilder value = new StringBuilder();
		value.append(bucketName).append('|').append(pathname).append('|').append(sign);
		if (StringUtils.isNotBlank(queryString)) {
			Assert.state(!queryString.contains("|"), "queryString不能包含|");
			value.append('|').append(queryString);
		}
		for (int i = 0; i < SHORT_URL_MAX_RETRY; i++) {
			String shortCode = generateShortCode();
			String key = SHORT_URL_KEY_PREFIX + shortCode;
			Boolean ok = stringRedisTemplate.opsForValue()
				.setIfAbsent(key, value.toString(), expireSeconds, TimeUnit.SECONDS);
			if (Boolean.TRUE.equals(ok)) {
				return shortCode;
			}
		}
		throw new IllegalStateException("生成短码失败，请重试");
	}
	
	public ShortUrlTarget resolveShortUrl(String shortCode) {
		Assert.notBlank(shortCode, "shortCode不能为空");
		String value = stringRedisTemplate.opsForValue().get(SHORT_URL_KEY_PREFIX + shortCode);
		if (value == null || value.isEmpty()) {
			return null;
		}
		String[] parts = value.split("\\|", 4);
		if (parts.length < 3 || parts[0].isEmpty() || parts[1].isEmpty() || parts[2].isEmpty()) {
			log.warn("短链接value格式非法:shortCode={},value={}", shortCode, value);
			return null;
		}
		String bucketName = parts[0];
		String pathname = parts[1];
		String sign = parts[2];
		String queryString = parts.length == 4 ? parts[3] : null;
		return new ShortUrlTarget(bucketName, pathname, sign, queryString);
	}
	
	private String generateShortCode() {
		StringBuilder sb = new StringBuilder(SHORT_URL_LENGTH);
		for (int i = 0; i < SHORT_URL_LENGTH; i++) {
			sb.append(SHORT_URL_CHARS.charAt(SECURE_RANDOM.nextInt(SHORT_URL_CHARS.length())));
		}
		return sb.toString();
	}
	
	@Data
	public static class ShortUrlTarget {
		private final String bucketName;
		private final String pathname;
		private final String sign;
		private final String queryString;
		
		public ShortUrlTarget(String bucketName, String pathname, String sign, String queryString) {
			this.bucketName = bucketName;
			this.pathname = pathname;
			this.sign = sign;
			this.queryString = queryString;
		}
	}
}
