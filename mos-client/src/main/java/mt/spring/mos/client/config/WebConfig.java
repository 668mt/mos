package mt.spring.mos.client.config;

import mt.spring.mos.client.entity.MosClientProperties;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

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
				List<MosClientProperties.BasePath> basePaths = mosClientProperties.getDetailBasePaths();
				if (basePaths != null) {
					String[] resourcePaths = new String[basePaths.size()];
					for (int i = 0; i < basePaths.size(); i++) {
						String basePath = basePaths.get(i).getPath();
						if (!basePath.endsWith("/")) {
							basePath = basePath + "/";
						}
						resourcePaths[i] = "file:" + basePath;
					}
					registry.addResourceHandler("/mos/**").addResourceLocations(resourcePaths)
							.resourceChain(true)
							.addTransformer((request, resource, transformerChain) -> {
								String encodeKey = request.getParameter("encodeKey");
								if (encodeKey != null) {
									resource = new EncodeResource(resource, encodeKey);
								}
								return resource;
							})
					;
				}
			}
			
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**").allowedHeaders("GET", "POST", "PUT", "DELETE");
			}
		};
	}
}
