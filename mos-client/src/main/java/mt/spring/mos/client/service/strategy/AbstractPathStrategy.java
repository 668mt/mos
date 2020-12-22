package mt.spring.mos.client.service.strategy;

import mt.spring.mos.client.entity.MosClientProperties;
import org.jetbrains.annotations.Nullable;

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
	
	public abstract String getBasePath(@Nullable String pathname, List<MosClientProperties.BasePath> basePaths, long fileSize);
	
	@Override
	public String getBasePath(long fileSize, @Nullable String pathname) {
		return getBasePath(pathname, mosClientProperties.getDetailBasePaths(), fileSize);
	}
	
	@Override
	public String getBasePath(long fileSize) {
		return getBasePath(null, mosClientProperties.getDetailBasePaths(), fileSize);
	}
}
