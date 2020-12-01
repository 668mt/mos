package mt.spring.mos.client.service.strategy;

import mt.spring.mos.base.algorithm.weight.WeightAlgorithm;
import mt.spring.mos.client.entity.MosClientProperties;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2020/11/29
 */
@Component
public class WeightStrategy extends AbstractPathStrategy {
	
	public static final String STRATEGY_NAME = "weight";
	
	public WeightStrategy(MosClientProperties mosClientProperties) {
		super(mosClientProperties);
	}
	
	@Override
	public String getBasePath(List<MosClientProperties.BasePath> basePaths, long fileSize) {
		List<MosClientProperties.BasePath> list = basePaths.stream().filter(basePath -> {
			long freeSpace = new File(basePath.getPath()).getFreeSpace();
			return freeSpace > fileSize && BigDecimal.valueOf(freeSpace).compareTo(mosClientProperties.getMinAvaliableSpaceGB().multiply(BigDecimal.valueOf(FileUtils.ONE_GB))) > 0;
		}).collect(Collectors.toList());
		Assert.notEmpty(list, "无可用存储服务器可用");
		return new WeightAlgorithm<MosClientProperties.BasePath>(list).weightRandom().getPath();
	}
	
	@Override
	public String getName() {
		return STRATEGY_NAME;
	}
}
