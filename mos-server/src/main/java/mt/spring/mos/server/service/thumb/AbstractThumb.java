package mt.spring.mos.server.service.thumb;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.tools.video.FfmpegUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2023/9/1
 */
@Slf4j
public abstract class AbstractThumb implements ThumbSupport {
	@Override
	public File createThumb(@NotNull Resource resource, @NotNull File tempDir, @NotNull File tempFile) throws Exception {
		tempDir.mkdirs();
		File thumbFile = new File(tempDir, "index.jpg");
		//截图
		log.info("开始截图");
		FfmpegUtils.screenShot(tempFile, thumbFile, getWidth(), getSeconds(), 20, TimeUnit.SECONDS);
		return thumbFile;
	}
}
