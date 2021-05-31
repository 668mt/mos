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
public class UploadMergeRequest {
	private String totalMd5;
	private long totalSize;
	private int chunks;
	private boolean isPublic;
	private String contentType;
	private String pathname;
	private boolean updateMd5;
	private boolean wait;
	private boolean cover;
	private long lastModified;
	
	public UploadMergeRequest(String totalMd5, long totalSize, int chunks, boolean updateMd5, long lastModified, UploadInfo uploadInfo) {
		this.lastModified = lastModified;
		this.totalMd5 = totalMd5;
		this.totalSize = totalSize;
		this.chunks = chunks;
		this.updateMd5 = updateMd5;
		this.wait = true;
		this.isPublic = uploadInfo.isPublic();
		this.contentType = uploadInfo.getContentType();
		this.pathname = uploadInfo.getPathname();
		this.cover = uploadInfo.isCover();
	}
	
	public HttpEntity buildEntity() {
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		ContentType contentType = ContentType.create("text/plain", Consts.UTF_8);
		builder.addPart("totalMd5", new StringBody(totalMd5, contentType));
		builder.addPart("totalSize", new StringBody(totalSize + "", contentType));
		builder.addPart("chunks", new StringBody(chunks + "", contentType));
		builder.addPart("isPublic", new StringBody(isPublic + "", contentType));
		if (StringUtils.isNotBlank(this.contentType)) {
			builder.addPart("contentType", new StringBody(this.contentType, contentType));
		}
		builder.addPart("pathname", new StringBody(pathname, contentType));
		builder.addPart("lastModified", new StringBody(lastModified + "", contentType));
		builder.addPart("cover", new StringBody(cover + "", contentType));
		builder.addPart("updateMd5", new StringBody(updateMd5 + "", contentType));
		builder.addPart("wait", new StringBody(wait + "", contentType));
		return builder.build();
	}
}
