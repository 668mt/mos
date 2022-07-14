package mt.spring.mos.server.service.fragment;

/**
 * @author Martin
 */
public interface FragmentJob<T> {
	/**
	 * 执行任务
	 *
	 * @param task 任务
	 */
	void doJob(T task);
}