package mt.spring.mos.sdk.upload;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author Martin
 * @Date 2020/11/25
 */
@Slf4j
public class TaskTimeWatch {
	@Getter
	private long start;
	@Getter
	private long end;
	
	public void start() {
		this.start = System.currentTimeMillis();
	}
	
	public void end() {
		this.end = System.currentTimeMillis();
	}
	
	public long getCostMills() {
		return this.end - this.start;
	}
}
