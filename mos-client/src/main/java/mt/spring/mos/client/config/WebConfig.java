package mt.spring.mos.client.config;

import mt.spring.mos.client.entity.MosClientProperties;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Author Martin
 * @Date 2020/5/15
 */
@Configuration
public class WebConfig {
	
	@Autowired
	private MosClientProperties mosClientProperties;
	
	@Bean
	public WebMvcConfigurer webMvcConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addResourceHandlers(@NotNull ResourceHandlerRegistry registry) {
				String[] basePaths = mosClientProperties.getBasePaths();
				if (basePaths != null) {
					String[] resourcePaths = new String[basePaths.length];
					for (int i = 0; i < basePaths.length; i++) {
						String basePath = basePaths[i];
						if (!basePath.endsWith("/")) {
							basePaths[i] = basePath + "/";
						}
						resourcePaths[i] = "file:" + basePaths[i];
					}
					registry.addResourceHandler("/mos/**").addResourceLocations(resourcePaths);
				}
			}
			
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**").allowedHeaders("GET", "POST", "PUT", "DELETE");
			}
		};
	}
}
