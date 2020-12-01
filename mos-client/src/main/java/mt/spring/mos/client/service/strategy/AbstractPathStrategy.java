package mt.spring.mos.client.service.strategy;

import mt.spring.mos.client.entity.MosClientProperties;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/11/30
 */
public abstract class AbstractPathStrategy implements PathStrategy {
	protected final MosClientProperties mosClientProperties;
	
	public AbstractPathStrategy(MosClientProperties mosClientProperties) {
		this.mosClientProperties = mosClientProperties;
	}
	
	public abstract String getBasePath(List<MosClientProperties.BasePath> basePaths, long fileSize);
	
	@Override
	public String getBasePath(long fileSize) {
		return getBasePath(mosClientProperties.getDetailBasePaths(), fileSize);
	}
}
