package mt.spring.mos.client.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import mt.spring.mos.base.algorithm.weight.WeightAble;
import mt.spring.mos.base.utils.RegexUtils;
import mt.spring.mos.client.service.strategy.WeightStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2020/5/15
 */
@ConfigurationProperties(prefix = "mos.client")
@Component
@Data
public class MosClientProperties {
	/**
	 * 存储路径
	 */
	private String[] basePaths;
	/**
	 * 空闲空间GB，如果剩余空间少于这个数，则不允许此路径作为上传路径
	 */
	private BigDecimal minAvaliableSpaceGB = BigDecimal.valueOf(2);
	
	private String[] serverHosts = new String[]{"http://localhost:9700"};
	private boolean enableAutoImport = false;
	private String registPwd;
	/**
	 * 文件合并核心线程数
	 */
	private Integer mergeThreadPoolCore = 4;
	
	@NestedConfigurationProperty
	private Instance instance = new Instance();
	
	private String basePathStrategyName = WeightStrategy.STRATEGY_NAME;
	
	public List<BasePath> getDetailBasePaths() {
		Assert.notNull(basePaths, "存储路径未配置");
		
		return Arrays.stream(basePaths).map(s -> {
			List<String[]> list = RegexUtils.findList(s, "\\((\\d+)\\)", new Integer[]{0, 1});
			int weight = 1;
			String path = s;
			if (!CollectionUtils.isEmpty(list)) {
				String[] group = list.get(list.size() - 1);
				weight = Integer.parseInt(group[1]);
				path = s.replace(group[0], "");
			}
			return new BasePath(path, weight);
		}).collect(Collectors.toList());
	}
	
	@Data
	public static class Instance {
		private String clientId;
		private String ip;
		private Integer port;
		private Integer weight;
		private String remark;
	}
	
	@Data
	@NoArgsConstructor
	public static class BasePath implements WeightAble {
		private String path;
		private Integer weight;
		
		public BasePath(String path, int weight) {
			this.path = path;
			this.weight = weight;
		}
	}
}
