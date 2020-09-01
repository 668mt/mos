package mt.common.starter.message;

import lombok.Data;
import mt.common.config.CommonProperties;
import mt.common.starter.message.annotation.EnableMessage;
import mt.common.starter.message.annotation.IgnoreMessage;
import mt.common.starter.message.utils.MessageUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;

/**
 * @Author Martin
 * @Date 2019/1/6
 */
@Data
public class MessageAspectAdapter {
	@Autowired
	private MessageUtils messageUtils;
	@Autowired
	private CommonProperties commonProperties;
	
	public MessageAspectAdapter() {
	}
	
	public MessageAspectAdapter(MessageUtils messageUtils, CommonProperties commonProperties) {
		this.messageUtils = messageUtils;
		this.commonProperties = commonProperties;
	}
	
	public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
		Signature signature = proceedingJoinPoint.getSignature();
		MethodSignature methodSignature = (MethodSignature) signature;
		Method targetMethod = methodSignature.getMethod();
		Object result = proceedingJoinPoint.proceed();
		if (result == null || result instanceof String) {
			return result;
		}
		if(result instanceof ResponseEntity){
			return result;
		}
		if (commonProperties.getMessager().getAutoMessage()) {
			//自动开启信息处理
			IgnoreMessage classIgnoreMessage = AnnotatedElementUtils.getMergedAnnotation(proceedingJoinPoint.getThis().getClass(), IgnoreMessage.class);
			IgnoreMessage methodIgnoreMessage = AnnotatedElementUtils.getMergedAnnotation(targetMethod, IgnoreMessage.class);
			if (classIgnoreMessage == null && methodIgnoreMessage == null) {
				return messageUtils.message(result);
			}
		} else {
			//手动开启信息处理
			EnableMessage classEnableMessage = AnnotatedElementUtils.getMergedAnnotation(proceedingJoinPoint.getThis().getClass(), EnableMessage.class);
			EnableMessage methodEnableMessage = AnnotatedElementUtils.getMergedAnnotation(targetMethod, EnableMessage.class);
			IgnoreMessage methodIgnoreMessage = AnnotatedElementUtils.getMergedAnnotation(targetMethod, IgnoreMessage.class);
			if (classEnableMessage != null || methodEnableMessage != null) {
				if (methodIgnoreMessage != null)
					return result;
				return messageUtils.message(result);
			}
		}
		return result;
	}
	
}