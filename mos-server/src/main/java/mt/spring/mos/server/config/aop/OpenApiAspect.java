package mt.spring.mos.server.config.aop;

import mt.common.currentUser.UserContext;
import mt.spring.mos.sdk.utils.MosEncrypt;
import mt.spring.mos.server.annotation.OpenApi;
import mt.spring.mos.server.entity.po.AccessControl;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.entity.po.User;
import mt.spring.mos.server.service.AccessControlService;
import mt.spring.mos.server.service.BucketGrantService;
import mt.spring.mos.server.service.BucketService;
import mt.spring.mos.server.service.ResourceService;
import mt.utils.Assert;
import mt.utils.MyUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
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
	private UserContext userContext;
	@Autowired
	private AccessControlService accessControlService;
	@Autowired
	private ResourceService resourceService;
	@Autowired
	private BucketGrantService bucketGrantService;
	
	@Before("@annotation(mt.spring.mos.server.annotation.OpenApi)")
	public void openApi(JoinPoint joinPoint) throws UnsupportedEncodingException {
		ServletRequestAttributes attributes = getRequestContext();
		assert attributes != null;
		HttpServletRequest request = attributes.getRequest();
		HttpServletResponse response = attributes.getResponse();
		assert response != null;
		
		Object[] args = joinPoint.getArgs();
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		Parameter[] parameters = method.getParameters();
		User currentUser = (User) userContext.getCurrentUser();
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
		
		if (sign != null) {
			List<String> pathnameList = new ArrayList<>();
			String[] pathnames = getParameter("pathnames", args, parameters, request, String[].class);
			if (StringUtils.isNotBlank(pathname)) {
				pathnameList.add(pathname);
			} else if (MyUtils.isNotEmpty(pathnames)) {
				pathnameList.addAll(Arrays.asList(pathnames));
			} else {
				throw new IllegalStateException("pathname不能为空");
			}
			for (String s : pathnameList) {
				Assert.state(!s.contains(".."), "非法路径" + s);
			}
			
			String names = StringUtils.join(pathnameList, ",");
			//校验签名
			MosEncrypt.MosEncryptContent mosEncryptContent = accessControlService.checkSign(sign, names, bucketName);
			AccessControl accessControl = accessControlService.findById(mosEncryptContent.getOpenId());
			Long bucketId = accessControl.getBucketId();
			bucket = bucketService.findById(bucketId);
			Assert.notNull(bucket, "资源不存在");
			if (!bucketGrantService.hasPerms(accessControl, bucket, openApi.perms())) {
				throwNoPermException(response);
			}
		} else if (currentUser != null) {
			bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		} else {
			Assert.state(pathname != null, "路径名不能为空");
			bucket = bucketService.findOne("bucketName", bucketName);
			Assert.notNull(bucket, "资源不存在");
			Resource resource = resourceService.findResourceByPathnameAndBucketId(pathname, bucket.getId());
			Assert.notNull(resource, "资源不存在");
			if (resource.getIsPublic() == null || !resource.getIsPublic()) {
				//无访问权限
				throwNoPermException(response);
			}
			//公共权限
		}
		Assert.notNull(bucket, "bucket不存在");
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i].getType().isAssignableFrom(Bucket.class)) {
				BeanUtils.copyProperties(bucket, args[i]);
			}
		}
	}
}
