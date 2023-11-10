package mt.spring.mos.server.service;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.utils.CollectionUtils;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.tools.video.FfmpegUtils;
import mt.spring.tools.video.entity.VideoInfo;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2022/3/12
 */
@Service
@Slf4j
public class VideoService {
	@Autowired
	@Lazy
	private ResourceService resourceService;
	@Autowired
	private MosServerProperties mosServerProperties;
	
	public void setVideoInfo(@NotNull Resource resource, @NotNull String pathname, @NotNull File tempFile) {
		Long during = resource.getDuring();
		String videoLength = resource.getVideoLength();
		if (during != null && StringUtils.isNotBlank(videoLength)) {
			//已经解析过
			return;
		}
		if (!isVideo(pathname)) {
			return;
		}
		
		long parsedDuring = 0;
		String parsedVideoLength = "";
		
		try {
			log.info("开始获取视频长度:{}", pathname);
			VideoInfo videoInfo = FfmpegUtils.getVideoInfo(tempFile, 20, TimeUnit.SECONDS);
			if (videoInfo != null) {
				parsedDuring = videoInfo.getDuring();
				parsedVideoLength = videoInfo.getVideoLength();
				log.info("获取视频长度成功:{},视频长度：{}", pathname, parsedVideoLength);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Resource updateResource = new Resource();
			updateResource.setId(resource.getId());
			updateResource.setDuring(parsedDuring);
			updateResource.setVideoLength(parsedVideoLength);
			resourceService.updateByIdSelective(updateResource);
		}
	}
	
	private boolean isVideo(String pathname) {
		List<String> suffixs = mosServerProperties.getFileSuffix().get("video");
		if (CollectionUtils.isEmpty(suffixs)) {
			return false;
		}
		for (String suffix : suffixs) {
			if (pathname.endsWith(suffix)) {
				return true;
			}
		}
		return false;
	}
}
