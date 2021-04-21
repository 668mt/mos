package mt.spring.mos.server.entity;

import lombok.Data;
import mt.spring.mos.server.service.strategy.CurrentPriorityWeightClientStragegy;
import mt.spring.mos.server.service.strategy.WeightClientStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
	private String domain;
	private Integer deleteRecentDaysNotUsed = 5;
	
	private Map<String, ContentTypeRender> defaultContentTypes;
	private Boolean convertTraditionalToFileHouse = true;
	private Long convertTraditionalToFileHouseSleepMills = -1L;
	
	private String clientStrategy = WeightClientStrategy.STRATEGY_NAME;
	
	private Integer backCronLimit = 1000;
	/**
	 * 设置当前ip，用于任务分片健康检查，不设置则自动获取，如自动获取且有多网卡时，请设置ipPrefix参数
	 */
	private String currentIp;
	/**
	 * 自动获取当前服务的ip，ip前缀，如192.168.0
	 */
	private String ipPrefix;
	private Integer asyncTaskThreadCore = 5;
	
	private CorsConfig corsConfig = new CorsConfig();
	private ArchiveConfig archive = new ArchiveConfig();
	private ClearConfig clear = new ClearConfig();
	
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
	
}
