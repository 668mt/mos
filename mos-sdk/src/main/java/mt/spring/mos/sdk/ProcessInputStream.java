package mt.spring.mos.sdk;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class ProcessInputStream extends InputStream {
	private final InputStream inputStream;
	private final int total;
	private int readed;
	private double percent;
	private final UpdateListener updateListener;
	
	public interface UpdateListener {
		void listen(double percent);
	}
	
	@SneakyThrows
	public ProcessInputStream(InputStream inputStream, UpdateListener updateListener) {
		this.inputStream = inputStream;
		this.updateListener = updateListener;
		total = inputStream.available();
	}
	
	public void update(int read) {
		readed += read;
		double updatePercent = 0;
		if (total == 0) {
			updatePercent = 1;
		} else {
			updatePercent = BigDecimal.valueOf(readed * 1.0 / total).setScale(2, RoundingMode.HALF_UP).doubleValue();
		}
		if (percent != updatePercent) {
			percent = updatePercent;
			if (updateListener != null) {
				updateListener.listen(percent);
			}
		}
	}
	
	@Override
	public int read(@NotNull byte[] b) throws IOException {
		int read = inputStream.read(b);
		update(read);
		return read;
	}
	
	@Override
	public int read(@NotNull byte[] b, int off, int len) throws IOException {
		int read = inputStream.read(b, off, len);
		update(read);
		return read;
	}
	
	@Override
	public int read() throws IOException {
		int read = inputStream.read();
		update(read);
		return read;
	}
	
	@Override
	public void close() throws IOException {
		inputStream.close();
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
	public synchronized void mark(int readlimit) {
		inputStream.mark(readlimit);
	}
	
	@Override
	public synchronized void reset() throws IOException {
		inputStream.reset();
	}
	
	@Override
	public boolean markSupported() {
		return inputStream.markSupported();
	}
}