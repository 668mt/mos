package mt.spring.mos.server.service;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.entity.VideoInfo;
import mt.spring.mos.base.utils.CollectionUtils;
import mt.spring.mos.base.utils.FfmpegUtils;
import mt.spring.mos.sdk.MosSdk;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Resource;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.net.URL;
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
	@Autowired
	@Lazy
	private AccessControlService accessControlService;
	
	public void setVideoInfo(@NotNull Bucket bucket, @NotNull Long resourceId) {
		Resource resource = resourceService.findResourceByIdAndBucketId(resourceId, bucket.getId());
		if (resource == null) {
			return;
		}
		Long during = resource.getDuring();
		String videoLength = resource.getVideoLength();
		if (during != null && StringUtils.isNotBlank(videoLength)) {
			//已经解析过
			return;
		}
		long parsedDuring = 0;
		String parsedVideoLength = "";
		
		try {
			VideoInfo videoInfo = getVideoInfo(resource, bucket.getBucketName());
			if (videoInfo != null) {
				parsedDuring = videoInfo.getDuring();
				parsedVideoLength = videoInfo.getVideoLength();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			resource.setDuring(parsedDuring);
			resource.setVideoLength(parsedVideoLength);
			resourceService.updateById(resource);
		}
	}
	
	public VideoInfo getVideoInfo(Resource resource, String bucketName) throws Exception {
		String pathname = resourceService.getPathname(resource);
		List<String> suffixs = mosServerProperties.getFileSuffix().get("video");
		if (CollectionUtils.isEmpty(suffixs)) {
			return null;
		}
		boolean match = false;
		for (String suffix : suffixs) {
			if (pathname.endsWith(suffix)) {
				match = true;
				break;
			}
		}
		if (!match) {
			return null;
		}
		MosSdk mosSdk = accessControlService.getMosSdk(0L, bucketName);
		String url = mosSdk.getUrl(pathname, 2, TimeUnit.HOURS);
		VideoInfo videoInfo = FfmpegUtils.getVideoInfo(new URL(url), 10, TimeUnit.MINUTES);
		if (videoInfo != null) {
			log.info("{}的视频长度是：{}", pathname, videoInfo.getVideoLength());
		} else {
			log.info("{}的视频长度解析失败", pathname);
		}
		return videoInfo;
	}
}
