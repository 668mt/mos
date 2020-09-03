package mt.spring.mos.client.entity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * @Author Martin
 * @Date 2020/5/15
 */
@ConfigurationProperties(prefix = "mos.client")
@Component
@Data
public class MosClientProperties {
	private String[] basePaths;
	/**
	 * 空闲空间GB，如果剩余空间少于这个数，则不允许此路径作为上传路径
	 */
	private BigDecimal minAvaliableSpaceGB = BigDecimal.valueOf(2);
	
	private String[] serverHosts = new String[]{"http://localhost:9700"};
	private boolean enableAutoImport = false;
	private String registPwd;
	
	@NestedConfigurationProperty
	private Instance instance = new Instance();
	
	@Data
	public static class Instance {
		private String clientId;
		private String ip;
		private Integer port;
		private Integer weight;
		private String remark;
	}
}
