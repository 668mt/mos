package mt.spring.mos.sdk.upload;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class UploadProcessListener {
	public abstract void update(double percent);
	
	private int chunks;
	private final AtomicInteger done = new AtomicInteger(0);
	public final double uploadWeight = 0.98;
	
	void init(int chunks) {
		this.chunks = chunks;
	}
	
	void addDone() {
		done.addAndGet(1);
		//uploadWeight * 分片上传进度
		update(BigDecimal.valueOf(uploadWeight).multiply(BigDecimal.valueOf(done.get()).divide(BigDecimal.valueOf(chunks), 2, RoundingMode.DOWN)).setScale(2, RoundingMode.DOWN).doubleValue());
	}
	
	void finish() {
		update(1);
	}
}