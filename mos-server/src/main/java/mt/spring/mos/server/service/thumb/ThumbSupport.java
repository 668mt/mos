package mt.spring.mos.server.service.thumb;

import mt.spring.mos.server.entity.po.Resource;
import org.jetbrains.annotations.NotNull;

import java.io.File;
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
	
	/**
	 * 创建缩略图
	 *
	 * @param resource 资源
	 * @param tempDir  临时目录
	 * @param tempFile 临时文件
	 * @return 缩略图文件
	 * @throws Exception 异常
	 */
	File createThumb(@NotNull Resource resource, @NotNull File tempDir, @NotNull File tempFile) throws Exception;
}
