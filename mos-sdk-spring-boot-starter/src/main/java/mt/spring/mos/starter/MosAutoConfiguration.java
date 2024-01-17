package mt.spring.mos.starter;

import mt.spring.mos.sdk.MosSdk;
import mt.spring.mos.sdk.entity.MosConfig;
import mt.spring.mos.sdk.entity.upload.MosUploadConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @Author Martin
 * @Date 2020/12/10
 */
@ConditionalOnProperty("mos.config.host")
@EnableConfigurationProperties(MosProperties.class)
public class MosAutoConfiguration {
	
	@Bean
	@ConditionalOnMissingBean(MosSdk.class)
	public MosSdk mosSdk(MosProperties mosProperties) {
		MosConfig mosConfig = mosProperties.getConfig();
		MosUploadConfig upload = mosProperties.getUpload();
		return new MosSdk(mosConfig, upload);
	}
}
