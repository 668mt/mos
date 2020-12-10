package mt.spring.mos.sdk.entity.upload;

import static mt.spring.mos.base.utils.IOUtils.MB;

/**
 * @Author Martin
 * @Date 2020/11/25
 */
public class MosUploadConfig {
	/**
	 * 期望的分片数，默认100
	 */
	private int expectChunks = 100;
	/**
	 * 最小的分片大小，单位byte，默认2MB
	 */
	private long minPartSize = 2 * MB;
	/**
	 * 最大的分片大小，单位byte，默认20MB
	 */
	private long maxPartSize = 20 * MB;
	/**
	 * 上传/下载线程池核心线程数，默认5
	 */
	private int threadPoolCore = 5;
	/**
	 * 上传/下载线程池最大队列容量，默认不限制
	 */
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
