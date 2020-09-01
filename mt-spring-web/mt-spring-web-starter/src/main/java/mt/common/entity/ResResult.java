package mt.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author Martin
 * @Date 2019/12/27
 */
@Data
@NoArgsConstructor
public class ResResult {
	private Status status;
	private String message;
	private Object result;
	
	@JsonIgnore
	public boolean isSuccess() {
		return status != null && status == Status.ok;
	}
	
	public enum Status {
		ok, error
	}
	
	public ResResult(Status status, String message) {
		this.status = status;
		this.message = message;
	}
	
	public ResResult(Object result) {
		this.status = Status.ok;
		this.result = result;
	}
	
	public static ResResult success(Object data) {
		return new ResResult(data);
	}
	
	public static ResResult success() {
		return new ResResult(Status.ok, null);
	}
	
	public static ResResult error(String message) {
		return new ResResult(Status.error, message);
	}
}
