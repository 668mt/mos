package mt.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

/**
 * @Author LIMAOTAO236
 * @Date 2018-9-7
 */
@ConfigurationProperties(prefix = "project")
@Data
@Component
public class CommonProperties {
	
	private String basePackage;
	private String[] daoPackage;
	
	/**
	 * 创建表的包名
	 */
	private String[] generateEntityPackages;
	/**
	 * 主键管理的表名
	 */
	private String idGenerateTableName = "idGenerate";
	/**
	 * 数据锁的表名
	 */
	private String dataLockTableName = "dataLock";
	private String loggingDaoPackageLevel = "DEBUG";
	private String loggingRedisLevel = "ERROR";
	
	/**
	 * 是否启用生成器
	 */
	private Boolean generatorEnable = true;
	
	/**
	 * 信息处理
	 */
	@NestedConfigurationProperty
	private Messager messager = new Messager();
	
	@Data
	public static class Messager {
		private String dealPackage = "mt";
		/**
		 * 自动开启信息处理
		 */
		private Boolean autoMessage = true;
	}
}
