package mt.common.utils;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;
import java.util.Map;

/**
 * Utils - Spring
 *
 * @author OIG Team
 * @version 5.0
 */
@Lazy(false)
@Component
public final class SpringUtils implements ApplicationContextAware, DisposableBean {
	
	/**
	 * ApplicationContext
	 */
	private static ApplicationContext applicationContext;
	
	/**
	 * 不可实例化
	 */
	private SpringUtils() {
	}
	
	/**
	 * 设置ApplicationContext
	 *
	 * @param applicationContext ApplicationContext
	 */
	public void setApplicationContext(ApplicationContext applicationContext) {
		SpringUtils.applicationContext = applicationContext;
	}
	
	/**
	 * 获取ApplicationContext
	 *
	 * @return ApplicationContext
	 */
	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}
	
	/**
	 * 获取实例
	 *
	 * @param name Bean名称
	 * @return 实例
	 */
	public static Object getBean(@NotNull String name) {
		return applicationContext.getBean(name);
	}
	
	public static String getProperty(@NotNull String name) {
		return applicationContext.getEnvironment().getProperty(name);
	}
	
	public static <T> T getProperty(String key, Class<T> type) {
		T property = getApplicationContext().getEnvironment().getProperty(key, type);
		return property;
	}
	
	/**
	 * 获取实例
	 *
	 * @param type Bean类型
	 * @return 实例
	 */
	public static <T> T getBean(@NotNull Class<T> type) {
		return applicationContext.getBean(type);
	}
	
	/**
	 * 获取实例
	 *
	 * @param name Bean名称
	 * @param type Bean类型
	 * @return 实例
	 */
	public static <T> T getBean(@NotNull String name, @NotNull Class<T> type) {
		return applicationContext.getBean(name, type);
	}
	
	/**
	 * 获取实例
	 *
	 * @param type Bean类型
	 * @return 实例
	 */
	public static <T> Map<String, T> getBeansOfType(@NotNull Class<T> type) {
		return applicationContext.getBeansOfType(type);
	}
	
	/**
	 * 获取国际化消息
	 *
	 * @param code 代码
	 * @param args 参数
	 * @return 国际化消息
	 */
	public static String getMessage(@NotNull String code, Object... args) {
		LocaleResolver localeResolver = getBean("localeResolver", LocaleResolver.class);
		Locale locale = localeResolver.resolveLocale(null);
		return applicationContext.getMessage(code, args, locale);
	}
	
	/**
	 * 销毁
	 */
	public void destroy() throws Exception {
		applicationContext = null;
	}
	
}