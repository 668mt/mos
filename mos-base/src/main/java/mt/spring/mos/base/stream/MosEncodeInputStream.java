package mt.spring.mos.base.stream;

import lombok.Getter;
import lombok.SneakyThrows;
import mt.spring.mos.base.utils.MosFileEncodeUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * @Author Martin
 * @Date 2020/12/1
 */
public class MosEncodeInputStream extends InputStream {
	private final InputStream inputStream;
	@Getter
	private int offset;
	
	@SneakyThrows
	public MosEncodeInputStream(InputStream inputStream, String key) {
		this.inputStream = inputStream;
		byte[] fileHead = MosFileEncodeUtils.getFileHead(key);
		offset = fileHead.length;
		inputStream.skip(offset);
	}
	
	@Override
	public int read(@NotNull byte[] b) throws IOException {
		return inputStream.read(b);
	}
	
	@Override
	public int read(@NotNull byte[] b, int off, int len) throws IOException {
		return inputStream.read(b, off, len);
	}
	
	@Override
	public long skip(long n) throws IOException {
		return inputStream.skip(n);
	}
	
	@Override
	public int available() throws IOException {
		return inputStream.available();
	}
	
	@Override
	public void close() throws IOException {
		inputStream.close();
	}
	
	@Override
	public synchronized void mark(int readlimit) {
		inputStream.mark(readlimit);
	}
	
	@Override
	public synchronized void reset() throws IOException {
		inputStream.reset();
		inputStream.skip(offset);
	}
	
	@Override
	public boolean markSupported() {
		return false;
	}
	
	@Override
	public int read() throws IOException {
		return inputStream.read();
	}
}
