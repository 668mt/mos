package mt.spring.mos.sdk.entity.upload;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;

/**
 * @Author Martin
 * @Date 2020/11/25
 */
@Data
public class UploadInitRequest {
	private String pathname;
	private String totalMd5;
	private long totalSize;
	private boolean cover;
	private int chunks;
	private long lastModified;
	private boolean isPublic;
	private String contentType;
	
	public UploadInitRequest(String totalMd5, long totalSize, int chunks, long lastModified, UploadInfo uploadInfo) {
		this.totalMd5 = totalMd5;
		this.totalSize = totalSize;
		this.chunks = chunks;
		this.contentType = uploadInfo.getContentType();
		this.pathname = uploadInfo.getPathname();
		this.cover = uploadInfo.isCover();
		this.isPublic = uploadInfo.isPublic();
	}
	
	public HttpEntity buildEntity() {
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		ContentType contentType = ContentType.create("text/plain", Consts.UTF_8);
		builder.addPart("pathname", new StringBody(pathname, contentType));
		builder.addPart("totalMd5", new StringBody(totalMd5, contentType));
		builder.addPart("totalSize", new StringBody(totalSize + "", contentType));
		builder.addPart("cover", new StringBody(cover + "", contentType));
		builder.addPart("chunks", new StringBody(chunks + "", contentType));
		builder.addPart("isPublic", new StringBody(isPublic + "", contentType));
		builder.addPart("lastModified", new StringBody(lastModified + "", contentType));
		if (StringUtils.isNotBlank(this.contentType)) {
			builder.addPart("contentType", new StringBody(this.contentType, contentType));
		}
		return builder.build();
	}
}
