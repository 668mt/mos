package mt.spring.mos.server.controller;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @Author Martin
 * @Date 2020/12/19
 */
public class ReadableOutputStream extends OutputStream {
	private final OutputStream outputStream;
	private long readed;
	private final ReadEvent readEvent;
	
	public interface ReadEvent {
		void onFlush(long readed);
		
		void onClose(long readed);
		
		void onException(long readed, IOException e);
	}
	
	public ReadableOutputStream(OutputStream outputStream, ReadEvent readEvent) {
		this.outputStream = outputStream;
		this.readEvent = readEvent;
	}
	
	@Override
	public void write(int b) throws IOException {
		try {
			outputStream.write(b);
			readed += b;
		} catch (IOException e) {
			readEvent.onException(readed, e);
			throw e;
		}
	}
	
	@Override
	public void write(@NotNull byte[] b) throws IOException {
		try {
			outputStream.write(b);
			readed += b.length;
		} catch (IOException e) {
			readEvent.onException(readed, e);
			throw e;
		}
	}
	
	@Override
	public void write(@NotNull byte[] b, int off, int len) throws IOException {
		try {
			outputStream.write(b, off, len);
			readed += len;
		} catch (IOException e) {
			readEvent.onException(readed, e);
			throw e;
		}
	}
	
	@Override
	public void flush() throws IOException {
		outputStream.flush();
		readEvent.onFlush(readed);
	}
	
	@Override
	public void close() throws IOException {
		outputStream.close();
		readEvent.onClose(readed);
	}
}
