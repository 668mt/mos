package mt.spring.mos.plugin.config;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2021/1/8
 */
@Data
public class UploadProperties {
	private String host;
	private Long openId;
	private String bucketName;
	private String secretKey;
	
	private boolean cover;
	private String srcFile;
	private String desPath;
	private String desName;
}
