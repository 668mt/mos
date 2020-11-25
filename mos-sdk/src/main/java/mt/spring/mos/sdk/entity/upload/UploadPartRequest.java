package mt.spring.mos.sdk.entity.upload;

import lombok.Data;
import mt.spring.mos.base.utils.MyInputStreamBody;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
	
	public HttpEntity buildEntity() {
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		ContentType contentType = ContentType.create("multipart/form-data", StandardCharsets.UTF_8);
		builder.setContentType(contentType);
		builder.addPart("file", new MyInputStreamBody(inputStream, contentType, "file", length));
		builder.addTextBody("pathname", pathname, contentType);
		builder.addTextBody("totalMd5", totalMd5, contentType);
		builder.addTextBody("totalSize", totalSize + "", contentType);
		builder.addTextBody("chunkMd5", chunkMd5, contentType);
		builder.addTextBody("chunkIndex", chunkIndex + "", contentType);
		return builder.build();
	}
}
