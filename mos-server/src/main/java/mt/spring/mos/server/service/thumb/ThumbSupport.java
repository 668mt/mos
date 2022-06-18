package mt.spring.mos.server.service.thumb;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/12/11
 */
public interface ThumbSupport {
	List<String> getSuffixs();
	
	default int getWidth() {
		return 300;
	}
	
	int getSeconds();
	
	default boolean match(String suffix) {
		List<String> suffixs = getSuffixs();
		return suffixs.contains(suffix.toLowerCase());
	}
}
