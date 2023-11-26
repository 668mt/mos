package mt.spring.mos.server.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * @Author Martin
 * @Date 2021/2/6
 */
@Configuration
public class WebMvcConfiguration {
	@Bean
	public WebMvcConfigurer mosWebMvcConfigurer(List<HandlerInterceptor> interceptors) {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(@NotNull CorsRegistry registry) {
				registry.addMapping("/**")
					.allowedOriginPatterns("*")
					.allowedMethods("*")
					.allowedHeaders("*")
					.allowCredentials(true);
			}
			
			@Override
			public void addInterceptors(@NotNull InterceptorRegistry registry) {
				for (HandlerInterceptor interceptor : interceptors) {
					registry.addInterceptor(interceptor).addPathPatterns("/**");
				}
			}
		};
	}
}
