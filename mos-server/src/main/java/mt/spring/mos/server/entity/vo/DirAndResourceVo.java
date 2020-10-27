package mt.spring.mos.server.entity.vo;

import lombok.Data;
import mt.spring.mos.server.utils.SizeUtils;
import mt.spring.mos.server.utils.UrlEncodeUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author Martin
 * @Date 2020/9/12
 */
@Data
public class DirAndResourceVo {
	private Boolean isDir;
	private Long id;
	private String path;
	private Long sizeByte;
	private Date createdDate;
	private String createdBy;
	private Date updatedDate;
	private String updatedBy;
	private String icon;
	public Boolean isPublic;
	private String contentType;
	
	public String getUrlEncodePath() {
		if (path == null) {
			return null;
		}
		return UrlEncodeUtils.encodePathname(path);
	}
	
	public static final Map<String, String> iconPatterns = new HashMap<>();
//	public Map<String, String> iconPatterns = new HashMap<>();
	
	static {
		iconPatterns.put("icon-tupian2", ".*\\.(jpg|jpeg|bmp|tif|png|gif)");
		iconPatterns.put("icon-word", ".*\\.(doc|docx)");
		iconPatterns.put("icon-PPT", ".*\\.(ppt|pptx)");
		iconPatterns.put("icon-excel", ".*\\.(xls|xlsx)");
		iconPatterns.put("icon-yunpanlogo-", ".*\\.(txt|xml)");
		iconPatterns.put("icon-script-language", ".*\\.(bat|sh|java|py)");
		iconPatterns.put("icon-shipin", ".*\\.(mp4|rmvb|avi|flv|3gp|mov|rm|mpg|mpeg)");
		iconPatterns.put("icon-SQLshengjiwenjian", ".*\\.(sql)");
		iconPatterns.put("icon-zip", ".*\\.(zip|rar|gz|tar)");
	}
	
	public String getIcon() {
		if (!isDir) {
			String fileName = getFileName();
			for (Map.Entry<String, String> entry : iconPatterns.entrySet()) {
				String regex = entry.getValue();
				if (fileName.matches(regex) || fileName.matches(regex.toUpperCase())) {
					return entry.getKey();
				}
			}
		}
		return null;
	}
	
	public String getReadableSize() {
		if (sizeByte == null) {
			return null;
		}
		return SizeUtils.getReadableSize(sizeByte);
	}
	
	public String getFileName() {
		if (path == null) {
			return null;
		}
		return new File(path).getName();
	}
	
	public Boolean getIsPublic() {
		if (isPublic == null) {
			return false;
		}
		return isPublic;
	}
	
}
