package mt.spring.mos.server.service.thumb;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.server.entity.MosServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2020/12/11
 */
@Component
@Slf4j
public class VideoThumb extends AbstractThumb {
	@Autowired
	private MosServerProperties mosServerProperties;
	
	@Override
	public List<String> getSuffixs() {
		List<String> video = mosServerProperties.getFileSuffix().get("video");
		if (video == null) {
			video = Arrays.asList(".mp4", ".m3u8", ".flv");
		}
		return video.stream().map(s -> s.startsWith(".") ? s : "." + s).collect(Collectors.toList());
	}
	
	@Override
	public int getSeconds() {
		return 10;
	}

//	@Override
//	public File createThumb(@NotNull Long resourceId, @NotNull String url) throws Exception {
//		File tempDir = new File(FileUtils.getTempDirectory(), "thumb/" + resourceId);
//		tempDir.mkdirs();
//		File tempVideoFile = new File(tempDir, "index.mp4");
//		File thumbFile = new File(tempDir, "index.jpg");
//		int seconds = getSeconds();
//		String time = Utils.buildTimeDuration((seconds + 5) * 1000L);
////		//先下载
////		log.info("下载视频前{}秒", time);
////		FfmpegUtils.cutVideo(url, tempVideoFile, "00:00:00", time, null);
//		//截图
//		log.info("开始截图");
//		FfmpegUtils.screenShot(new MultimediaObject(new URL(url)), thumbFile, getWidth(), seconds, 30, TimeUnit.SECONDS);
//		if (thumbFile.exists()) {
//			log.info("视频截图成功");
//			return thumbFile;
//		} else {
//			log.info("视频截图失败");
//		}
//		return null;
//	}
//
//	@Override
//	public void cleanTemp(@NotNull Long resourceId) {
//		File tempDir = new File(FileUtils.getTempDirectory(), "thumb/" + resourceId);
//		FileUtils.deleteQuietly(tempDir);
//	}
}
