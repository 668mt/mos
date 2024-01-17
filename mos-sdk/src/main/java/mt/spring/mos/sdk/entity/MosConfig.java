package mt.spring.mos.sdk.entity;

import lombok.Data;
import mt.spring.mos.sdk.utils.HostChooseUtils;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/11/25
 */
@Data
public class MosConfig {
	/**
	 * mos服务器地址，如：http://localhost:9700
	 */
	private List<String> hosts;
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
	private Long openId;
	private Integer maxTotalConnections = 1024;
	private Integer maxConnectionsPerRoute = 1024;
	
	public MosConfig() {
	}
	
	public MosConfig(String host, String bucketName, String secretKey, Long openId) {
		this(List.of(host), bucketName, secretKey, openId);
	}
	
	public MosConfig(List<String> hosts, String bucketName, String secretKey, Long openId) {
		this.hosts = hosts;
		this.bucketName = bucketName;
		this.secretKey = secretKey;
		this.openId = openId;
	}
	
	public String getHost() {
		return HostChooseUtils.getAvailableHostByWeight(hosts, "/index.html");
	}
}
