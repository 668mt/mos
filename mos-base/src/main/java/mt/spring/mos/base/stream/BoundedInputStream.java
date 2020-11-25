package mt.spring.mos.base.stream;

import java.io.IOException;
import java.io.InputStream;

public class BoundedInputStream extends InputStream {
	
	/**
	 * the wrapped input stream
	 */
	private final InputStream in;
	
	/**
	 * the max length to provide
	 */
	private final long max;
	
	/**
	 * the number of bytes already returned
	 */
	private long pos = 0;
	
	/**
	 * the marked position
	 */
	private long mark = -1;
	
	/**
	 * flag if close shoud be propagated
	 */
	private boolean propagateClose = true;
	
	/**
	 * Creates a new <code>BoundedInputStream</code> that wraps the given input
	 * stream and limits it to a certain size.
	 *
	 * @param in   The wrapped input stream
	 * @param size The maximum number of bytes to return
	 */
	public BoundedInputStream(InputStream in, long size) {
		// Some badly designed methods - eg the servlet API - overload length
		// such that "-1" means stream finished
		this.max = size;
		this.in = in;
	}
	
	/**
	 * Creates a new <code>BoundedInputStream</code> that wraps the given input
	 * stream and is unlimited.
	 *
	 * @param in The wrapped input stream
	 */
	public BoundedInputStream(InputStream in) {
		this(in, -1);
	}
	
	/**
	 * Invokes the delegate's <code>read()</code> method if
	 * the current position is less than the limit.
	 *
	 * @return the byte read or -1 if the end of stream or
	 * the limit has been reached.
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public int read() throws IOException {
		if (max >= 0 && pos >= max) {
			return -1;
		}
		int result = in.read();
		pos++;
		return result;
	}
	
	/**
	 * Invokes the delegate's <code>read(byte[])</code> method.
	 *
	 * @param b the buffer to read the bytes into
	 * @return the number of bytes read or -1 if the end of stream or
	 * the limit has been reached.
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public int read(byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}
	
	/**
	 * Invokes the delegate's <code>read(byte[], int, int)</code> method.
	 *
	 * @param b   the buffer to read the bytes into
	 * @param off The start offset
	 * @param len The number of bytes to read
	 * @return the number of bytes read or -1 if the end of stream or
	 * the limit has been reached.
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (max >= 0 && pos >= max) {
			return -1;
		}
		long maxRead = max >= 0 ? Math.min(len, max - pos) : len;
		int bytesRead = in.read(b, off, (int) maxRead);
		
		if (bytesRead == -1) {
			return -1;
		}
		
		pos += bytesRead;
		return bytesRead;
	}
	
	/**
	 * Invokes the delegate's <code>skip(long)</code> method.
	 *
	 * @param n the number of bytes to skip
	 * @return the actual number of bytes skipped
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public long skip(long n) throws IOException {
		long toSkip = max >= 0 ? Math.min(n, max - pos) : n;
		long skippedBytes = in.skip(toSkip);
		pos += skippedBytes;
		return skippedBytes;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int available() throws IOException {
		if (max >= 0 && pos >= max) {
			return 0;
		}
		return in.available();
	}
	
	/**
	 * Invokes the delegate's <code>toString()</code> method.
	 *
	 * @return the delegate's <code>toString()</code>
	 */
	@Override
	public String toString() {
		return in.toString();
	}
	
	/**
	 * Invokes the delegate's <code>close()</code> method
	 * if {@link #isPropagateClose()} is {@code true}.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public void close() throws IOException {
		if (propagateClose) {
			in.close();
		}
	}
	
	/**
	 * Invokes the delegate's <code>reset()</code> method.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public synchronized void reset() throws IOException {
		in.reset();
		pos = mark;
	}
	
	/**
	 * Invokes the delegate's <code>mark(int)</code> method.
	 *
	 * @param readlimit read ahead limit
	 */
	@Override
	public synchronized void mark(int readlimit) {
		in.mark(readlimit);
		mark = pos;
	}
	
	/**
	 * Invokes the delegate's <code>markSupported()</code> method.
	 *
	 * @return true if mark is supported, otherwise false
	 */
	@Override
	public boolean markSupported() {
		return in.markSupported();
	}
	
	/**
	 * Indicates whether the {@link #close()} method
	 * should propagate to the underling {@link InputStream}.
	 *
	 * @return {@code true} if calling {@link #close()}
	 * propagates to the <code>close()</code> method of the
	 * underlying stream or {@code false} if it does not.
	 */
	public boolean isPropagateClose() {
		return propagateClose;
	}
	
	/**
	 * Set whether the {@link #close()} method
	 * should propagate to the underling {@link InputStream}.
	 *
	 * @param propagateClose {@code true} if calling
	 *                       {@link #close()} propagates to the <code>close()</code>
	 *                       method of the underlying stream or
	 *                       {@code false} if it does not.
	 */
	public void setPropagateClose(boolean propagateClose) {
		this.propagateClose = propagateClose;
	}
	
	/**
	 * Get original input stream
	 *
	 * @return original input stream
	 */
	public InputStream getWrappedInputStream() {
		return this.in;
	}
	
	/**
	 * Go back current position
	 *
	 * @param backoff
	 */
	protected void backoff(long backoff) {
		this.pos -= backoff;
	}
	
}
