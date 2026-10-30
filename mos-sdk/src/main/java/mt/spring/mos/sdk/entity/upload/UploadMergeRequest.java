package mt.spring.mos.sdk.entity.upload;

import lombok.Data;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;

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
	
	public RequestBody buildEntity() {
		MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
		builder.addFormDataPart("totalMd5", totalMd5);
		builder.addFormDataPart("totalSize", totalSize + "");
		builder.addFormDataPart("chunks", chunks + "");
		builder.addFormDataPart("isPublic", isPublic + "");
		if (StringUtils.isNotBlank(this.contentType)) {
			builder.addFormDataPart("contentType", this.contentType);
		}
		builder.addFormDataPart("pathname", pathname);
		builder.addFormDataPart("lastModified", lastModified + "");
		builder.addFormDataPart("cover", cover + "");
		builder.addFormDataPart("updateMd5", updateMd5 + "");
		builder.addFormDataPart("wait", wait + "");
		return builder.build();
	}
}
