package mt.spring.mos.sdk.entity.upload;

import lombok.Data;
import mt.spring.mos.base.stream.MyInputStreamBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import java.io.InputStream;

/**
 * @Author Martin
 * @Date 2020/11/25
 */
@Data
public class UploadPartRequest {
	private String pathname;
	private String totalMd5;
	private long totalSize;
	private String chunkMd5;
	private int chunkIndex;
	private InputStream inputStream;
	private long length;
	
	public UploadPartRequest(String pathname, String totalMd5, long totalSize, String chunkMd5, int chunkIndex, InputStream inputStream, long length) {
		this.pathname = pathname;
		this.totalMd5 = totalMd5;
		this.totalSize = totalSize;
		this.chunkMd5 = chunkMd5;
		this.chunkIndex = chunkIndex;
		this.inputStream = inputStream;
		this.length = length;
	}
	
	public RequestBody buildEntity() {
		MediaType contentType = MediaType.parse("multipart/form-data; charset=utf-8");
		MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
		builder.addFormDataPart("file", "file", new MyInputStreamBody(inputStream, contentType, "file", length));
		builder.addFormDataPart("pathname", pathname);
		builder.addFormDataPart("totalMd5", totalMd5);
		builder.addFormDataPart("totalSize", totalSize + "");
		builder.addFormDataPart("chunkMd5", chunkMd5);
		builder.addFormDataPart("chunkIndex", chunkIndex + "");
		return builder.build();
	}
}
