package mt.spring.mos.sdk.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一台 mos 服务器的地址信息
 *
 * 配置格式（hosts 里逗号分隔的每一项）：
 * <pre>
 *   http://localhost:9700                              单地址
 *   http://localhost:9700[http://rs1.com]              带外网地址
 *   http://localhost:9700(70)                          带权重
 *   http://localhost:9700[http://rs1.com](70)          带外网地址 + 权重
 * </pre>
 *
 * @Author Martin
 * @Date 2026/10/30
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Host {
	/**
	 * 内网地址：配置里 [] 前面的地址，没配 [] 时就是整个地址。
	 * SDK 调用 mos-server（上传/下载/列目录等）和健康检查都用这个地址
	 */
	private String host;
	/**
	 * 外网地址：配置里 [] 中的地址，没配 [] 时为 null。
	 * 只用于拼给最终用户（外网）的链接
	 */
	private String externHost;
	/**
	 * 权重，缺省 100；0 表示不参与选择
	 */
	private Double weight;

	public Host(String host, String externHost, int weight) {
		this(host, externHost, (double) weight);
	}

	/**
	 * 对外地址：优先外网地址，没配外网地址时回退到内网地址
	 *
	 * @return
	 */
	public String getExternHostOrDefault() {
		return externHost != null ? externHost : host;
	}
}
