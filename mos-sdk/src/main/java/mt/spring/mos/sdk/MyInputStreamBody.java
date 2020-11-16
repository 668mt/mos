package mt.spring.mos.sdk;

import lombok.SneakyThrows;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.InputStreamBody;

import java.io.InputStream;

/**
 * @Author Martin
 * @Date 2020/11/16
 */
public class MyInputStreamBody extends InputStreamBody {
	
	public MyInputStreamBody(InputStream in, ContentType contentType, String filename) {
		super(in, contentType, filename);
	}
	
	@SneakyThrows
	@Override
	public long getContentLength() {
		return getInputStream().available();
	}
}
