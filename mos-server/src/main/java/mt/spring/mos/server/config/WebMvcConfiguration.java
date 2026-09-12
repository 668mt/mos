package mt.spring.mos.server.config;

import mt.spring.mos.server.intercept.LanRedirectInterceptor;
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
	/**
	 * mos 公开资源接口的重定向拦截路径（与 OpenMosController 的 @GetMapping 一致）
	 */
	private static final String OPEN_MOS_PATH_PATTERN = "/mos/{bucketName}/**";

	@Bean
	public WebMvcConfigurer mosWebMvcConfigurer(List<HandlerInterceptor> interceptors,
											   LanRedirectInterceptor lanRedirectInterceptor) {
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
				// 先注册内网/公网重定向拦截器，仅作用于公开资源接口路径，避免误伤管理端/上传端
				registry.addInterceptor(lanRedirectInterceptor).addPathPatterns(OPEN_MOS_PATH_PATTERN);
				// 其余通用拦截器继续作用于 /**
				for (HandlerInterceptor interceptor : interceptors) {
					if (interceptor instanceof LanRedirectInterceptor) {
						continue;
					}
					registry.addInterceptor(interceptor).addPathPatterns("/**");
				}
			}
		};
	}
}
