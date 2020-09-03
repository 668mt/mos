package mt.spring.mos.client.entity;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2020/5/15
 */
@Data
public class ResResult {
	public static final String ERROR = "error";
	public static final String RUNNING = "running";
	public static final String OK = "ok";
	public static final String Valid = "warn";

	public ResResult() {
		this.status = OK;
	}

	public ResResult(Object result) {
		this.status = OK;
		this.result = result;
	}

	public ResResult(String status, String message) {
		this.status = status;
		this.message = message;
	}

	private String status;
	private String message;
	private Object result;
}
