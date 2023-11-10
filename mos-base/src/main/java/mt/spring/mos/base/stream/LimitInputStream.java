package mt.spring.mos.base.stream;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;

public class LimitInputStream extends InputStream {
	private final InputStream proxy;
	private int index;
	private final long size;
	@Getter
	private long totalRead;
	
	public LimitInputStream(InputStream proxy, long limitKbSeconds) {
		this.proxy = proxy;
		size = (long) (limitKbSeconds * 1024 / 1000 * 1.5);
	}
	
	@SneakyThrows
	@Override
	public int read() throws IOException {
		int read = proxy.read();
		index++;
		totalRead++;
		if (index >= size) {
			Thread.sleep(1);
			index = 0;
		}
		return read;
	}
	
}