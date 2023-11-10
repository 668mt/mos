package mt.spring.mos.server.service.thumb;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * @Author Martin
 * @Date 2020/12/11
 */
@Component
public class ImageThumb extends AbstractThumb {
	@Override
	public List<String> getSuffixs() {
		return Arrays.asList(".jpg", ".jpeg", ".png", ".tif", ".tiff", ".wbmp", ".jpe", ".gif", ".bmp");
	}
	
	@Override
	public int getSeconds() {
		return 0;
	}

//	@Override
//	public File createThumb(@NotNull Long resourceId, @NotNull String url) throws Exception {
//		File tempDir = new File(FileUtils.getTempDirectory(), "thumb/" + resourceId);
//		tempDir.mkdirs();
//		File thumbFile = new File(tempDir, "index.jpg");
//		//截图
//		FfmpegUtils.screenShot(new MultimediaObject(new URL(url)), thumbFile, getWidth(), getSeconds(), 30, TimeUnit.SECONDS);
//		return thumbFile;
//	}
//
//	@Override
//	public void cleanTemp(@NotNull Long resourceId) {
//		File tempDir = new File(FileUtils.getTempDirectory(), "thumb/" + resourceId);
//		FileUtils.deleteQuietly(tempDir);
//	}
}
