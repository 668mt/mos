package mt.spring.mos.server.exception;

import org.jetbrains.annotations.NotNull;

/**
 * @Author Martin
 * @Date 2023/4/5
 */
public class NoThumbBizException extends BizException {
	private static final long serialVersionUID = -8911207082956772414L;
	
	public NoThumbBizException(@NotNull String message) {
		super(message);
	}
}
