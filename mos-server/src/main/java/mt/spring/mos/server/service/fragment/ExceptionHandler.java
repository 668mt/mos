package mt.spring.mos.server.service.fragment;

/**
 * @author Martin
 */
public interface ExceptionHandler<T> {
	/**
	 * 处理异常
	 *
	 * @param task 任务
	 * @param e    异常
	 */
	void handleException(T task, Exception e);
}