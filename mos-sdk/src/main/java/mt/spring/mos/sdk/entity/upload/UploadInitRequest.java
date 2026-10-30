package mt.spring.mos.sdk.entity.upload;

import lombok.Data;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

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
		this.lastModified = lastModified;
	}
	
	public RequestBody buildEntity() {
		MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
		builder.addFormDataPart("pathname", pathname);
		builder.addFormDataPart("totalMd5", totalMd5);
		builder.addFormDataPart("totalSize", totalSize + "");
		builder.addFormDataPart("cover", cover + "");
		builder.addFormDataPart("chunks", chunks + "");
		builder.addFormDataPart("isPublic", isPublic + "");
		builder.addFormDataPart("lastModified", lastModified + "");
		if (contentType != null && !contentType.isEmpty()) {
			builder.addFormDataPart("contentType", contentType);
		}
		return builder.build();
	}
}
