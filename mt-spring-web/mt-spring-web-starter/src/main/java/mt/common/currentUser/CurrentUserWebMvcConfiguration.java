package mt.common.currentUser;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * @Author Martin
 * @Date 2019/8/24
 */
@Configuration
@ConditionalOnBean(UserContext.class)
public class CurrentUserWebMvcConfiguration {
	
	@Bean
	public CurrentUserMethodArgumentResolver currentUserMethodArgumentResolver() {
		return new CurrentUserMethodArgumentResolver();
	}
	
	@Bean
	CurrentUserHandlerInterceptor currentUserHandlerInterceptor() {
		return new CurrentUserHandlerInterceptor();
	}
	
	@Bean
	public WebMvcConfigurer currentUserWebMvcConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addInterceptors(InterceptorRegistry registry) {
				registry.addInterceptor(currentUserHandlerInterceptor());
			}
			
			@Override
			public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
				resolvers.add(currentUserMethodArgumentResolver());
			}
		};
	}
}
