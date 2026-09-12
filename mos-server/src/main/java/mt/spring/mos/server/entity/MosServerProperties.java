package mt.spring.mos.server.entity;

import lombok.Data;
import mt.spring.mos.server.intercept.PublicIpResolver;
import mt.spring.mos.server.service.strategy.CurrentPriorityWeightClientStragegy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2020/5/19
 */
@ConfigurationProperties(prefix = "mos.server")
@Data
@Component
@RefreshScope
public class MosServerProperties {
	/**
	 * 备份超时时间
	 */
	private Integer backReadTimeout = 30 * 60 * 1000;
	/**
	 * 空闲空间GB，如果剩余空间少于这个数，则不允许上传
	 */
	private BigDecimal minAvaliableSpaceGB = BigDecimal.valueOf(4);
	private String defaultBucketName = "default";
	
	private String adminUsername = "admin";
	private String adminPassword = "admin";
	private String registPwd;
	private Integer deleteRecentDaysNotUsed = 5;
	
	private Map<String, ContentTypeRender> defaultContentTypes;
	private Boolean convertTraditionalToFileHouse = true;
	private Long convertTraditionalToFileHouseSleepMills = -1L;
	
	private String clientStrategy = CurrentPriorityWeightClientStragegy.STRATEGY_NAME;
	
	private Integer backCronLimit = 1000;
	private Double backCpuIdePercent = 0.5;
	/**
	 * 设置当前ip，用于任务分片健康检查，不设置则自动获取，如自动获取且有多网卡时，请设置ipPrefix参数
	 */
	private String currentIp;
	/**
	 * 自动获取当前服务的ip，ip前缀，如192.168.0
	 */
	private String ipPrefix;
	private Integer asyncTaskThreadCore = 5;
	/**
	 * 备份限速10MB/S
	 */
	private Integer backNetWorkLimitSpeed = 10;
	private Integer metaNetWorkLimitSpeed = 20;
	
	private CorsConfig corsConfig = new CorsConfig();
	private ArchiveConfig archive = new ArchiveConfig();
	private ClearConfig clear = new ClearConfig();
	/**
	 * 内网/公网重定向策略配置
	 */
	private LanRedirectConfig lanRedirect = new LanRedirectConfig();
	/**
	 * 文件后缀
	 */
	private Map<String, List<String>> fileSuffix;
	private String redisPrefix = "mos-server";
	
	@Data
	public static class ArchiveConfig {
		private Boolean enabled = false;
		private Integer beforeDays = 31;
		private String cron = "0 0 2 * * ?";
	}
	
	@Data
	public static class ClearConfig {
		private String cron = "0 0 3 * * ?";
		private Boolean auditLogEnabled = true;
		private Integer auditLogBeforeDays = 31;
		
		private Boolean archiveEnabled = true;
		private Integer archiveBeforeDays = 60;
		
		private Boolean workLogEnabled = true;
		private Integer workLogBeforeDays = 30;
	}
	
	@Data
	public static class CorsConfig {
		private List<String> allowedOrigins = Arrays.asList("*");
		private List<String> allowedHeaders = Arrays.asList("*");
	}
	
	@Data
	public static class ContentTypeRender {
		private List<String> patterns;
		private String value;
	}
	
	/**
	 * 内网/公网重定向策略配置：mos.server.lan-redirect.*
	 * 域名白名单命中后，同局域网客户端重定向到内网地址（复用 mos.server.currentIp + server.port），
	 * 不同局域网客户端重定向到公网地址（publicIp 未配置时按需懒加载）。
	 */
	@Data
	public static class LanRedirectConfig {
		/**
		 * 总开关：是否启用内网/公网重定向；默认 false（关闭），避免对现有用户产生影响。
		 */
		private Boolean enabled = false;
		/**
		 * 启用重定向策略的域名白名单（仅域名访问才会触发重定向，IP 直连放行）
		 */
		private List<String> domains;
		/**
		 * 公网 IP（可选）；未配置时按需懒加载
		 */
		private String publicIp;
		/**
		 * 公网端口（可选）；未配置时退化为 server.port
		 */
		private Integer publicPort;
		/**
		 * 公网 IP 查询端点 URL（可选）
		 */
		private String publicIpEndpoint;
		
		/**
		 * 是否启用重定向（默认 false，未显式启用时一律放行）
		 */
		public boolean isEnabled() {
			return Boolean.TRUE.equals(enabled);
		}
		
		/**
		 * 解析最终使用的公网端口：优先返回配置值，否则使用入参的默认端口（通常为 server.port）。
		 *
		 * @param defaultPort 默认端口（通常为 server.port）
		 * @return 公网端口
		 */
		public Integer resolvePublicPort(Integer defaultPort) {
			return publicPort != null ? publicPort : defaultPort;
		}
		
		/**
		 * 解析最终使用的公网 IP 查询端点：优先返回配置值，否则返回默认端点。
		 */
		public String resolvePublicIpEndpoint() {
			return publicIpEndpoint;
		}
		
		/**
		 * 解析最终使用的公网 IP：优先返回配置值，否则走后台刷新的缓存（由 PublicIpResolver 调度器填充）。
		 *
		 * @return 公网 IP；配置未指定且后台尚未刷新出有效值时返回 null
		 */
		public String resolvePublicIp() {
			if (publicIp != null && !publicIp.isEmpty()) {
				return publicIp;
			}
			return PublicIpResolver.getCurrentPublicIp();
		}
	}
	
	
}
