package mt.spring.mos.server.config.aop;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.server.annotation.DistributeJob;
import mt.spring.mos.server.service.LockService;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @Author Martin
 * @Date 2020/12/9
 */
@Aspect
@Component
@Slf4j
public class DistributeJobAspect extends AbstractAspect {
	@Autowired
	private LockService lockService;
	
	
	@Around("@annotation(mt.spring.mos.server.annotation.DistributeJob)")
	public Object doAround(ProceedingJoinPoint joinPoint) {
		Method method = getMethod(joinPoint);
		String name = method.getName();
		DistributeJob distributeJob = AnnotatedElementUtils.findMergedAnnotation(method, DistributeJob.class);
		Assert.notNull(distributeJob, "distributeJob不能为空");
		String key = distributeJob.key();
		if (StringUtils.isBlank(key)) {
			key = joinPoint.getTarget().getClass().getName() + "." + name;
		}
		lockService.tryLock(key, () -> {
			try {
				joinPoint.proceed();
			} catch (Throwable throwable) {
				throw new RuntimeException(throwable);
			}
		});
		return null;
	}
}
