package mt.spring.mos.client.config;

import lombok.SneakyThrows;
import mt.spring.mos.base.stream.MosEncodeInputStream;
import mt.spring.mos.base.utils.MosFileEncodeUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.resource.HttpResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

/**
 * @Author Martin
 * @Date 2020/12/2
 */
public class EncodeResource implements Resource {
	private Resource resource;
	private String key;
	private byte[] bytes;
	
	@SneakyThrows
	public EncodeResource(Resource resource, String key) {
		this.resource = resource;
		this.key = key;
		bytes = MosFileEncodeUtils.getFileHead(key);
	}
	
	@Override
	public boolean exists() {
		return resource.exists();
	}
	
	@Override
	public URL getURL() throws IOException {
		return resource.getURL();
	}
	
	@Override
	public URI getURI() throws IOException {
		return resource.getURI();
	}
	
	@Override
	public File getFile() throws IOException {
		return resource.getFile();
	}
	
	@Override
	public long contentLength() throws IOException {
		return resource.contentLength() - bytes.length;
	}
	
	@Override
	public long lastModified() throws IOException {
		return resource.lastModified();
	}
	
	@Override
	public Resource createRelative(String relativePath) throws IOException {
		return resource.createRelative(relativePath);
	}
	
	@Override
	public String getFilename() {
		return resource.getFilename();
	}
	
	@Override
	public String getDescription() {
		return resource.getDescription();
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		return new MosEncodeInputStream(resource.getInputStream(), key);
	}
	
}
