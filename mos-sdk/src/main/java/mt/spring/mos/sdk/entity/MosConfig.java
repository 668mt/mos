package mt.spring.mos.sdk.entity;

import lombok.Data;
import mt.spring.mos.sdk.utils.Host;
import mt.spring.mos.sdk.utils.HostChooseUtils;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/11/25
 */
@Data
public class MosConfig {
	/**
	 * mos服务器地址，如：
	 * 1. 单地址：http://localhost:9700
	 * 2. 多地址：http://localhost:9700,http://localhost:9701
	 * 3. 多地址带外网地址：http://localhost:9700[http://rs1.com],http://localhost:9701[http://rs2.com]
	 * 4. 多地址带权重：http://localhost:9700(70),http://localhost:9701(30)
	 * 5. 多地址带权重带外网地址：http://localhost:9700[http://rs1.com](70),http://localhost:9701[http://rs2.com](30)
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
	
	public MosConfig(String bucketName, String secretKey, Long openId) {
		this.bucketName = bucketName;
		this.secretKey = secretKey;
		this.openId = openId;
	}
	
	/**
	 * 可用的服务器内网地址，SDK 调用 mos-server 用这个
	 *
	 * @return
	 */
	public String getHost() {
		Host host = getHostInfo();
		return host == null ? null : host.getHost();
	}
	
	/**
	 * 可用的服务器信息（含内网/外网地址）
	 *
	 * @return 没有可用服务器时返回 null
	 */
	public Host getHostInfo() {
		if (hosts == null || hosts.isEmpty()) {
			return null;
		}
		return HostChooseUtils.getAvailableHostByWeight(hosts, "/index.html");
	}
	
	/**
	 * 对外地址：优先用配置里的外网地址（[] 中的），没配时回退到内网地址
	 *
	 * @return
	 */
	public String getDomain() {
		Host host = getHostInfo();
		return host == null ? null : host.getExternHostOrDefault();
	}
}
