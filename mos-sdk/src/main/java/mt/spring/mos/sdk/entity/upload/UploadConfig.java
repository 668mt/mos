package mt.spring.mos.sdk.entity.upload;

import static mt.spring.mos.base.utils.IOUtils.MB;

/**
 * @Author Martin
 * @Date 2020/11/25
 */
public class UploadConfig {
	private int expectChunks = 100;
	private long minPartSize = MB;
	private long maxPartSize = 20 * MB;
	private int threadPoolCore = 5;
	private int maxQueueSize = Integer.MAX_VALUE;
	
	public int getExpectChunks() {
		return Integer.getInteger("mos.upload.expectChunks", expectChunks);
	}
	
	public void setExpectChunks(int expectChunks) {
		this.expectChunks = expectChunks;
	}
	
	public long getMinPartSize() {
		return Long.getLong("mos.upload.minPartSize", minPartSize);
	}
	
	public void setMinPartSize(long minPartSize) {
		this.minPartSize = minPartSize;
	}
	
	public long getMaxPartSize() {
		return Long.getLong("mos.upload.maxPartSize", maxPartSize);
	}
	
	public void setMaxPartSize(long maxPartSize) {
		this.maxPartSize = maxPartSize;
	}
	
	public int getThreadPoolCore() {
		return Integer.getInteger("mos.upload.threadPoolCore", threadPoolCore);
	}
	
	public void setThreadPoolCore(int threadPoolCore) {
		this.threadPoolCore = threadPoolCore;
	}
	
	public int getMaxQueueSize() {
		return Integer.getInteger("mos.upload.maxQueueSize", maxQueueSize);
	}
	
	public void setMaxQueueSize(int maxQueueSize) {
		this.maxQueueSize = maxQueueSize;
	}
}
