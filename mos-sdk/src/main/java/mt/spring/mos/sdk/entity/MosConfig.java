package mt.spring.mos.sdk.entity;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2020/11/25
 */
@Data
public class MosConfig {
	/**
	 * mos服务器地址，如：http://localhost:9700
	 */
	private String host;
	/**
	 * 桶名称
	 */
	private String bucketName;
	/**
	 * 私钥
	 */
	private String secretKey;
	/**
	 * openId
	 */
	private long openId;
	
	public MosConfig() {
	}
	
	public MosConfig(String host, String bucketName, String secretKey, long openId) {
		this.host = host;
		this.bucketName = bucketName;
		this.secretKey = secretKey;
		this.openId = openId;
	}
}
