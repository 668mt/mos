package mt.spring.mos.server.service.thumb;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * @Author Martin
 * @Date 2020/12/11
 */
@Component
public class VideoThumb implements ThumbSupport {
	@Override
	public List<String> getSuffixs() {
		return Arrays.asList(".mp4", ".ts", ".avi", ".flv", ".mov");
	}
	
	@Override
	public int getSeconds() {
		return 0;
	}
}
