package mt.spring.mos.server.service.thumb;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.tools.video.FfmpegUtils;
import mt.spring.tools.video.entity.VideoInfo;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
	
	@Override
	public File createThumb(@NotNull Resource resource, @NotNull File tempDir, @NotNull File tempFile) throws Exception {
		tempDir.mkdirs();
		File thumbFile = new File(tempDir, "index.jpg");
		//截图
		log.info("开始截图");
		VideoInfo videoInfo = FfmpegUtils.getVideoInfo(tempFile, 2, TimeUnit.MINUTES);
		long during = videoInfo.getDuring();
		int seconds = Math.min(getSeconds(), (int) (during / 1000));
		FfmpegUtils.screenShot(tempFile, thumbFile, getWidth(), seconds, 20, TimeUnit.SECONDS);
		return thumbFile;
	}
	
}
