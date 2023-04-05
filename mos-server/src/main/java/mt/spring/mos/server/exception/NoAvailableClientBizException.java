package mt.spring.mos.server.exception;

import org.jetbrains.annotations.NotNull;

/**
 * @Author Martin
 * @Date 2023/4/5
 */
public class NoAvailableClientBizException extends BizException {
	public NoAvailableClientBizException(@NotNull String message) {
		super(message);
	}
}
