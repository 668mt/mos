package mt.spring.mos.server.config.upload;

import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;

/**
 * @Author Martin
 * @Date 2019/12/28
 */
@Configuration
public class UploadConfiguration {
	
	@Bean(name = "multipartResolver")
	public MultipartResolver multipartResolver(MultipartProperties multipartProperties) {
		CustomMultipartResolver customMultipartResolver = new CustomMultipartResolver();
		customMultipartResolver.setMaxInMemorySize((int) multipartProperties.getFileSizeThreshold().toBytes());
		customMultipartResolver.setMaxUploadSize(multipartProperties.getMaxRequestSize().toBytes());
		customMultipartResolver.setMaxUploadSizePerFile(multipartProperties.getMaxFileSize().toBytes());
		return customMultipartResolver;
	}
}
