package mt.spring.mos.server.exception;

import org.jetbrains.annotations.NotNull;

/**
 * @Author Martin
 * @Date 2023/4/5
 */
public class ResourceNotExistsException extends BizException {
	public ResourceNotExistsException(@NotNull String message) {
		super("404", message);
	}
}
