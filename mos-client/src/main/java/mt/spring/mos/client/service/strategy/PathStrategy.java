package mt.spring.mos.client.service.strategy;

import org.jetbrains.annotations.Nullable;

/**
 * @Author Martin
 * @Date 2020/11/29
 */
public interface PathStrategy {
	String getBasePath(long fileSize, @Nullable String pathname);
	
	String getBasePath(long fileSize);
	
	String getName();
}
