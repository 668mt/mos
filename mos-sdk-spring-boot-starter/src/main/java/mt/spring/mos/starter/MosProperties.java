package mt.spring.mos.starter;

import lombok.Data;
import mt.spring.mos.sdk.entity.MosConfig;
import mt.spring.mos.sdk.entity.upload.MosUploadConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * @Author Martin
 * @Date 2020/12/10
 */
@Data
@ConfigurationProperties(prefix = "mos")
public class MosProperties {
	@NestedConfigurationProperty
	private MosConfig config;
	@NestedConfigurationProperty
	private MosUploadConfig upload = new MosUploadConfig();
}
