package mt.spring.mos.server.utils;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author Martin
 * @Date 2020/10/27
 */
public class UrlEncodeUtils {
	public static void main(String[] args) throws Exception {
//		System.out.println(decode("http://192.168.29.1:9800/mos/10/mc/txt/test/%E6%9C%AA%E6%A0%87%E9%A2%98-1%2B%26+-+%E5%89%AF%E6%9C%AC.jpg"));
		DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory();
		uriFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.URI_COMPONENT);
		String url = "http://192.168.29.1:9800/mos/10/mc/txt/test/测试+这是2& - 副本.txt";
		URI uri = uriFactory.expand(url);
		URL url1 = new URL(url);
//		RestTemplate restTemplate = new RestTemplate();
//		String result = restTemplate.getForObject("http://192.168.29.1:9800/mos/10/mc/txt/test/测试+这是2& - 副本.txt", String.class);
//		System.out.println(result);
//		URI uri = new URI(url);
		System.out.println(uri.toString());
		System.out.println(url1.toString());
	}
	
	public static String base64Encode(String content) {
		return Base64.getUrlEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
	}
	
	public static String base64Decode(String content) {
		return new String(Base64.getUrlDecoder().decode(content), StandardCharsets.UTF_8);
	}
	
	public static String encodePathname(String desPathname) {
		return Stream.of(desPathname.split("/")).map(s -> {
			try {
				return URLEncoder.encode(s, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}).collect(Collectors.joining("/"));
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
