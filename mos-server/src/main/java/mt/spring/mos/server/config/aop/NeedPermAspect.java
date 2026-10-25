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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
		MosContext mosContext = new MosContext();
		MosContext.setContext(mosContext);
		mosContext.setBucketName(bucketName);
		NeedPerm needPerm = AnnotatedElementUtils.findMergedAnnotation(method, NeedPerm.class);
		Assert.notNull(needPerm, "needPerm不能为空");
		if (currentUser != null) {
			Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
			if (bucket == null) {
				/*
				 * 兼容客户端把 bucketName 同时放在 query 与 body 的情况：
				 * Spring 绑定普通参数时会把同名多值合并成数组，再由 ArrayToString 拼成 "a,b"，
				 * 导致这里按 "a,b" 查不到桶。取第一段再试一次。
				 */
				String firstSegment = firstSegment(bucketName);
				if (!firstSegment.equals(bucketName)) {
					bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), firstSegment);
					if (bucket != null) {
						bucketName = firstSegment;
						mosContext.setBucketName(bucketName);
					}
				}
			}
			Assert.notNull(bucket, "bucket不存在:" + bucketName + ",currentUserId:" + currentUser.getId());
			mosContext.setCurrentUserId(currentUser.getId());
			mosContext.setBucketId(bucket.getId());
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

	/**
	 * 取逗号分隔的第一段。
	 *
	 * 客户端把同一个参数同时放在 query 与 body 时，Spring 会把多值拼成 "a,b"，
	 * 取第一段即可还原出真正的参数值。
	 */
	private String firstSegment(String value) {
		if (value == null) {
			return null;
		}
		int idx = value.indexOf(',');
		return idx > 0 ? value.substring(0, idx) : value;
	}
}
