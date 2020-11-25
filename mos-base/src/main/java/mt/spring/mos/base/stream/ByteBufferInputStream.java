package mt.spring.mos.base.stream;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @Author Martin
 * @Date 2020/11/25
 */
public class ByteBufferInputStream extends InputStream {
	protected final ByteBuffer byteBuffer;
	
	public ByteBufferInputStream(ByteBuffer buf) {
		byteBuffer = buf;
		byteBuffer.flip();
	}
	
	@Override
	public int available() {
		return byteBuffer.remaining();
	}
	
	@Override
	public int read() throws IOException {
		return byteBuffer.hasRemaining() ? (byteBuffer.get() & 0xFF) : -1;
	}
	
	@Override
	public int read(@NotNull byte[] bytes, int off, int len) throws IOException {
		if (!byteBuffer.hasRemaining()) {
			return -1;
		}
		len = Math.min(len, byteBuffer.remaining());
		byteBuffer.get(bytes, off, len);
		return len;
	}
	
	@Override
	public void close() throws IOException {
		byteBuffer.clear();
	}
	
	@Override
	public synchronized void reset() throws IOException {
		byteBuffer.position(0);
	}
	
	@Override
	public synchronized void mark(int readlimit) {
		byteBuffer.mark();
	}
}
