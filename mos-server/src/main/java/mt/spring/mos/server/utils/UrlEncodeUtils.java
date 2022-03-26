package mt.spring.mos.server.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author Martin
 * @Date 2020/10/27
 */
public class UrlEncodeUtils {
	public static String base64Encode(String content) {
		return Base64.getUrlEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
	}
	
	public static String base64Decode(String content) {
		return new String(Base64.getUrlDecoder().decode(content), StandardCharsets.UTF_8);
	}
	
	public static String encodePathname(String desPathname) {
		int index = desPathname.indexOf("?");
		String queryString = "";
		if (index != -1) {
			queryString = desPathname.substring(index);
			desPathname = desPathname.substring(0, index);
		}
		return Stream.of(desPathname.split("/")).map(s -> {
			try {
				return URLEncoder.encode(s, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}).collect(Collectors.joining("/")) + queryString;
	}
	
	public static String encode(String content) {
		try {
			return URLEncoder.encode(content, String.valueOf(StandardCharsets.UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String decode(String content) {
		try {
			return URLDecoder.decode(content, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
}
