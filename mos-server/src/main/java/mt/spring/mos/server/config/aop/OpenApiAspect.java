package mt.spring.mos.server.config.aop;

import mt.spring.mos.sdk.utils.MosEncrypt;
import mt.spring.mos.server.annotation.OpenApi;
import mt.spring.mos.server.config.MosUserContext;
import mt.spring.mos.server.entity.po.AccessControl;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.entity.po.User;
import mt.spring.mos.server.service.AccessControlService;
import mt.spring.mos.server.service.BucketGrantService;
import mt.spring.mos.server.service.BucketService;
import mt.spring.mos.server.service.ResourceService;
import mt.utils.ReflectUtils;
import mt.utils.common.Assert;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Author Martin
 * @Date 2020/6/3
 */
@Aspect
@Component
public class OpenApiAspect extends AbstractAspect {
	@Autowired
	private BucketService bucketService;
	@Autowired
	private MosUserContext userContext;
	@Autowired
	private AccessControlService accessControlService;
	@Autowired
	private ResourceService resourceService;
	@Autowired
	private BucketGrantService bucketGrantService;
	
	public Object getValue(Object o, String path) throws Exception {
		String[] split = path.split("\\.");
		Object value = o;
		for (String s : split) {
			Field field = ReflectUtils.findField(value.getClass(), s);
			field.setAccessible(true);
			value = field.get(value);
		}
		return value;
	}
	
	@Around("@annotation(mt.spring.mos.server.annotation.OpenApi)")
	public Object openApi(ProceedingJoinPoint joinPoint) throws Throwable {
		ServletRequestAttributes attributes = getRequestContext();
		assert attributes != null;
		HttpServletRequest request = attributes.getRequest();
		HttpServletResponse response = attributes.getResponse();
		assert response != null;
		MosContext mosContext = new MosContext();
		
		Object[] args = joinPoint.getArgs();
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		Parameter[] parameters = method.getParameters();
		User currentUser = userContext.getCurrentUser();
		String sign = getParameter("sign", args, parameters, request, String.class);
		String bucketName = getParameter("bucketName", args, parameters, request, String.class);
		Assert.notBlank(bucketName, "未传入bucketName");
		
		//校验签名
		Bucket bucket;
		String pathname = getParameter("pathname", args, parameters, request, String.class);
		OpenApi openApi = method.getAnnotation(OpenApi.class);
		Assert.notNull(openApi, "openApi不能为空");
		if (StringUtils.isNotBlank(openApi.pathnamePrefix())) {
			String prefix = openApi.pathnamePrefix();
			prefix = prefix.replace("{bucketName}", bucketName);
			pathname = request.getRequestURI().substring(prefix.length());
			pathname = URLDecoder.decode(pathname, "UTF-8");
		}
		
		bucket = bucketService.findOne("bucketName", bucketName);
		Assert.notNull(bucket, "资源不存在");
		
		mosContext.setPathname(pathname);
		mosContext.setBucketId(bucket.getId());
		boolean pass = false;
		if (pathname != null && !"/".equals(pathname)) {
			Resource resource = resourceService.findResourceByPathnameAndBucketId(pathname, bucket.getId());
			if (resource != null && resource.getIsPublic()) {
				//公共权限
				pass = true;
			}
		}
		
		if (!pass) {
			if (sign != null) {
				List<String> pathnameList = new ArrayList<>();
				String[] pathnames = getParameter("pathnames", args, parameters, request, String[].class);
				if (StringUtils.isNotBlank(pathname)) {
					pathnameList.add(pathname);
				} else if (ArrayUtils.isNotEmpty(pathnames)) {
					pathnameList.addAll(Arrays.asList(pathnames));
				} else {
					throw new IllegalStateException("pathname不能为空");
				}
				for (String s : pathnameList) {
					Assert.state(!s.contains(".."), "非法路径" + s);
				}
				
				String names = StringUtils.join(pathnameList, ",");
				mosContext.setPathname(names);
				//校验签名
				MosEncrypt.MosEncryptContent mosEncryptContent = accessControlService.checkSign(sign, names, bucketName);
				AccessControl accessControl = accessControlService.findById(mosEncryptContent.getOpenId());
				mosContext.setOpenId(accessControl.getOpenId());
				mosContext.setExpireSeconds(mosEncryptContent.getExpireSeconds());
				Long bucketId = accessControl.getBucketId();
				Assert.state(bucket.getId().equals(bucketId), "bucket校验错误");
				if (!bucketGrantService.hasPerms(accessControl, bucket, openApi.perms())) {
					throwNoPermException(response, names);
				}
				pass = true;
			} else if (currentUser != null) {
				mosContext.setCurrentUserId(currentUser.getId());
				Bucket findBucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
				Assert.state(findBucket != null && bucket.getId().equals(findBucket.getId()), "bucket校验错误");
				pass = true;
			}
		}
		
		if (!pass) {
			throwNoPermException(response, pathname);
		}
		
		//签名验证完毕
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i].getType().equals(Bucket.class)) {
				BeanUtils.copyProperties(bucket, args[i]);
			}
		}
		
		MosContext.setContext(mosContext);
		try {
			return joinPoint.proceed();
		} finally {
			MosContext.clear();
		}
	}
}
