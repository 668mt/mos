package mt.spring.mos.sdk.entity;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2020/11/25
 */
@Data
public class MosConfig {
	private String host;
	private String bucketName;
	private String secretKey;
	private long openId;
	
	public MosConfig(String host, String bucketName, String secretKey, long openId) {
		this.host = host;
		this.bucketName = bucketName;
		this.secretKey = secretKey;
		this.openId = openId;
	}
}
