package mt.spring.mos.server.service.thumb;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * @Author Martin
 * @Date 2020/12/11
 */
@Component
public class ImageThumb implements ThumbSupport {
	@Override
	public List<String> getSuffixs() {
		return Arrays.asList(".jpg", ".jpeg", ".png", ".tif", ".tiff", ".wbmp", ".jpe", ".gif", ".bmp");
	}
	
	@Override
	public int getSeconds() {
		return 0;
	}
}
