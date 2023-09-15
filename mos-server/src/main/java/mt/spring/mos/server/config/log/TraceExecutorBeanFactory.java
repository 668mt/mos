package mt.spring.mos.server.config.log;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

/**
 * @Author Martin
 * @Date 2023/9/15
 */
@Component
public class TraceExecutorBeanFactory implements BeanPostProcessor {
	@Override
	public Object postProcessBeforeInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
		if (bean instanceof ExecutorService) {
			return new TraceExecutor((ExecutorService) bean);
		}
		return bean;
	}
}
