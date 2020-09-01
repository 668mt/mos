package mt.common.starter.message;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * @author limaotao236
 * @date 2019/12/18
 * @email limaotao236@pingan.com.cn
 */
@Aspect
public class DefaultMessageAspect extends MessageAspectAdapter {
	
	@Around("@annotation(org.springframework.web.bind.annotation.RequestMapping)")
	public Object doRequestMappingAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
		return super.doAround(proceedingJoinPoint);
	}
	
	@Around("@annotation(org.springframework.web.bind.annotation.GetMapping)")
	public Object doGetMappingAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
		return super.doAround(proceedingJoinPoint);
	}
	
	@Around("@annotation(org.springframework.web.bind.annotation.PostMapping)")
	public Object doPostMappingAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
		return super.doAround(proceedingJoinPoint);
	}
	
	@Around("@annotation(org.springframework.web.bind.annotation.PutMapping)")
	public Object doPutMappingAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
		return super.doAround(proceedingJoinPoint);
	}
	
	@Around("@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
	public Object doDeleteMappingAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
		return super.doAround(proceedingJoinPoint);
	}
	
	@Around("@annotation(org.springframework.web.bind.annotation.PatchMapping)")
	public Object doPatchMappingAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
		return super.doAround(proceedingJoinPoint);
	}
}
