package mt.spring.mos.base.stream;

import lombok.SneakyThrows;
import mt.spring.mos.base.utils.MosFileEncodeUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @Author Martin
 * @Date 2020/12/1
 */
public class MosEncodeOutputStream extends OutputStream {
	private final OutputStream outputStream;
	
	@SneakyThrows
	public MosEncodeOutputStream(OutputStream outputStream, String key) {
		this.outputStream = outputStream;
		outputStream.write(MosFileEncodeUtils.getFileHead(key));
	}
	
	@Override
	public void write(int b) throws IOException {
		outputStream.write(b);
	}
	
	@Override
	public void write(@NotNull byte[] b) throws IOException {
		outputStream.write(b);
	}
	
	@Override
	public void write(@NotNull byte[] b, int off, int len) throws IOException {
		outputStream.write(b, off, len);
	}
	
	@Override
	public void flush() throws IOException {
		outputStream.flush();
	}
	
	@Override
	public void close() throws IOException {
		outputStream.close();
	}
}
