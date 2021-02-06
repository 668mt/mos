//package mt.spring.mos.server.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
///**
// * @Author Martin
// * @Date 2021/2/6
// */
//@Configuration
//public class WebMvcConfiguration {
//	@Bean
//	public WebMvcConfigurer mosWebMvcConfigurer() {
//		return new WebMvcConfigurer() {
//			@Override
//			public void addCorsMappings(CorsRegistry registry) {
//				registry.addMapping("/**")
//						.allowedOrigins("*")
//						.allowedMethods("*")
//						.allowedHeaders("*")
//						.allowCredentials(true);
//			}
//		};
//	}
//}
