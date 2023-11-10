package mt.spring.mos.server.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

/**
 * @Author Martin
 * @Date 2023/4/5
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class BizException extends RuntimeException {
	private static final long serialVersionUID = -8911207082956772414L;
	
	private String code;
	private String message;
	
	public BizException(@NotNull String code, @NotNull String message) {
		this.code = code;
		this.message = message;
	}
	
	public BizException(@NotNull String message) {
		this.code = "500";
		this.message = message;
	}
}
