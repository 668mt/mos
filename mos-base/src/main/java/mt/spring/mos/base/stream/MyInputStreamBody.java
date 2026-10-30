package mt.spring.mos.base.stream;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.Source;

import java.io.IOException;
import java.io.InputStream;

/**
 * 带已知长度的输入流请求体（okhttp）
 *
 * 流式写出，不会把内容读进内存；按给定的 length 精确写出。
 *
 * @Author Martin
 * @Date 2020/11/16
 */
public class MyInputStreamBody extends RequestBody {
	private final InputStream in;
	private final MediaType contentType;
	private final String filename;
	private final long length;
	
	public MyInputStreamBody(InputStream in, MediaType contentType, String filename, long length) {
		this.in = in;
		this.contentType = contentType;
		this.filename = filename;
		this.length = length;
	}
	
	public String getFilename() {
		return filename;
	}
	
	@Override
	public MediaType contentType() {
		return contentType;
	}
	
	@Override
	public long contentLength() {
		return length;
	}
	
	@Override
	public void writeTo(BufferedSink sink) throws IOException {
		Source source = null;
		try {
			source = okio.Okio.source(in);
			long remaining = length;
			Buffer buffer = new Buffer();
			while (remaining > 0) {
				long read = source.read(buffer, remaining);
				if (read == -1) {
					break;
				}
				sink.write(buffer, read);
				remaining -= read;
			}
		} finally {
			if (source != null) {
				source.close();
			}
		}
	}
}
