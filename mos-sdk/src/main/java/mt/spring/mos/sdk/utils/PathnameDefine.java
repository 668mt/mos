package mt.spring.mos.sdk.utils;

import lombok.Data;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.base.utils.RegexUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author Martin
 * @Date 2022/9/7
 */
@Data
public class PathnameDefine {
	private String pathname;
	private String urlEncodedPathname;
	private boolean render;
	private boolean thumb;
	private boolean gallary;
	
	public PathnameDefine(String origin){
		Set<String> params = new HashSet<>();
		String pathname = origin;
		if (origin.startsWith("@")) {
			String[] group = RegexUtils.findFirst(origin, "^@(.+?)@*:(.+)$", new Integer[]{1, 2});
			Assert.notNull(group, "pathname格式不正确");
			params.addAll(Arrays.asList(group[0].split("-")));
			pathname = group[1];
		}
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		this.pathname = pathname;
		if(params.contains("render")){
			render = true;
		}else if(params.contains("thumb")){
			thumb = true;
		}else if(params.contains("gallary") || params.contains("gallery")){
			gallary = true;
		}
		
		this.urlEncodedPathname = Stream.of(pathname.split("/")).map(s -> {
			try {
				return URLEncoder.encode(s, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}).collect(Collectors.joining("/"));
	}
	
	public String getUrl(String host,String bucket,String sign){
		StringBuilder url = new StringBuilder(host +
				"/mos/" +
				bucket +
				urlEncodedPathname +
				"?sign=" +
				sign);
		if (gallary) {
			return String.format("%s/viewer/gallery?bucket=%s&path=%s&sign=%s", host, bucket,urlEncodedPathname, sign);
		} else {
			List<String> params = new ArrayList<>();
			if(render){
				params.add("render");
			}
			if(thumb){
				params.add("thumb");
			}
			for (String param : params) {
				url.append("&").append(param).append("=true");
			}
			return url.toString();
		}
	}
}
