package mt.spring.mos.server.service.thumb;

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
public class VideoThumb implements ThumbSupport {
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
}
