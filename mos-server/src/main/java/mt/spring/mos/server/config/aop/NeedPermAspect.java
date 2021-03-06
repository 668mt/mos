package mt.spring.mos.server.config.aop;

import mt.spring.mos.server.annotation.NeedPerm;
import mt.spring.mos.server.config.MosUserContext;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.User;
import mt.spring.mos.server.service.BucketGrantService;
import mt.spring.mos.server.service.BucketService;
import mt.utils.common.Assert;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @Author Martin
 * @Date 2020/12/9
 */
@Aspect
@Component
public class NeedPermAspect extends AbstractAspect {
	@Autowired
	private BucketGrantService bucketGrantService;
	@Autowired
	private MosUserContext userContext;
	@Autowired
	private BucketService bucketService;
	
	@Before("@annotation(mt.spring.mos.server.annotation.OpenApi)")
	public void beforeOpenApi(JoinPoint joinPoint) {
		beforeRequest(joinPoint);
	}
	
	@Before("@annotation(mt.spring.mos.server.annotation.NeedPerm)")
	public void beforeRequest(JoinPoint joinPoint) {
		ServletRequestAttributes attributes = getRequestContext();
		assert attributes != null;
		HttpServletRequest request = attributes.getRequest();
		HttpServletResponse response = attributes.getResponse();
		assert response != null;
		Object[] args = joinPoint.getArgs();
		Method method = getMethod(joinPoint);
		Parameter[] parameters = method.getParameters();
		User currentUser = userContext.getCurrentUser();
		String bucketName = getParameter("bucketName", args, parameters, request, String.class);
		Assert.notBlank(bucketName, "未传入bucketName");
		NeedPerm needPerm = AnnotatedElementUtils.findMergedAnnotation(method, NeedPerm.class);
		Assert.notNull(needPerm, "needPerm不能为空");
		if (currentUser != null) {
			Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
			Assert.notNull(bucket, "bucket不存在");
			boolean hasPerms = bucketGrantService.hasPerms(currentUser.getId(), bucket, needPerm.perms());
			if (!hasPerms) {
				throwNoPermException(response, bucketName);
			}
			for (int i = 0; i < parameters.length; i++) {
				if (parameters[i].getType().equals(Bucket.class)) {
					BeanUtils.copyProperties(bucket, args[i]);
				}
			}
		}
	}
}
