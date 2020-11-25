package mt.spring.mos.sdk.upload;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author Martin
 * @Date 2020/11/25
 */
@Slf4j
public class TaskTimeWatch {
	private long start;
	private final String name;
	
	public TaskTimeWatch(String name) {
		this.name = name;
	}
	
	public void start() {
		this.start = System.currentTimeMillis();
	}
	
	public void end() {
		long end = System.currentTimeMillis();
		log.info("{}用时：" + ((end - start) / 1000) + "s", name);
	}
}
