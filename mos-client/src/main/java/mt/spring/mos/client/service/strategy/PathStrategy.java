package mt.spring.mos.client.service.strategy;

/**
 * @Author Martin
 * @Date 2020/11/29
 */
public interface PathStrategy {
	String getBasePath(long fileSize);
	
	String getName();
}
