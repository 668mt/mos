package mt.spring.mos.server.service.fragment;

/**
 * @author Martin
 */
public interface FragmentIdFunction<T> {
	/**
	 * 获取分片id
	 *
	 * @param task 任务
	 * @return 分片id
	 */
	long getFragmentId(T task);
}