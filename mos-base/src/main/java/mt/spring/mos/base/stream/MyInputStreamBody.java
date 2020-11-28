package mt.spring.mos.base.stream;

import lombok.SneakyThrows;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.InputStreamBody;

import java.io.InputStream;

/**
 * @Author Martin
 * @Date 2020/11/16
 */
public class MyInputStreamBody extends InputStreamBody {
	private final long length;
	
	public MyInputStreamBody(InputStream in, ContentType contentType, String filename, long length) {
		super(in, contentType, filename);
		this.length = length;
	}
	
	@SneakyThrows
	@Override
	public long getContentLength() {
		return length;
	}
}
