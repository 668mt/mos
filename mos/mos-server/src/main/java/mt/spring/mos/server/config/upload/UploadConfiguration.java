package mt.spring.mos.server.config.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;

/**
 * @Author Martin
 * @Date 2019/12/28
 */
@Configuration
public class UploadConfiguration {
	@Value("${spring.multipart.maxInMemorySize:100}")
	private Integer maxInMemorySizeMb;
	@Value("${spring.multipart.maxUploadSize:4096}")
	private Integer maxUploadSizeMb;
	@Value("${spring.multipart.maxUploadSizePerFile:4096}")
	private Integer maxUploadSizePerFileMb;
	
	@Bean(name = "multipartResolver")
	public MultipartResolver multipartResolver() {
		CustomMultipartResolver customMultipartResolver = new CustomMultipartResolver();
		customMultipartResolver.setMaxInMemorySize(1024 * 1024 * maxInMemorySizeMb);
		customMultipartResolver.setMaxUploadSize(1024L * 1024 * maxUploadSizeMb);
		customMultipartResolver.setMaxUploadSizePerFile(1024L * 1024 * maxUploadSizePerFileMb);
		return customMultipartResolver;
	}
}
