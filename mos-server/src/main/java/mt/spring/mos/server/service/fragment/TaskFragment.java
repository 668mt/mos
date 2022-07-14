package mt.spring.mos.server.service.fragment;

import java.util.Collection;

/**
 * @Author Martin
 * @Date 2022/7/14
 */
public interface TaskFragment {
	
	/**
	 * 获取当前节点的分片信息
	 *
	 * @return 当前节点分片信息
	 */
	FragmentInfo getCurrentFragmentInfo();
	
	/**
	 * 任务分片
	 *
	 * @param tasks              任务集合
	 * @param fragmentIdFunction 获取分片id
	 * @param job                执行任务
	 * @param <T>                任务类
	 */
	<T> void fragment(Collection<T> tasks, FragmentIdFunction<T> fragmentIdFunction, FragmentJob<T> job);
	
	/**
	 * 任务分片
	 *
	 * @param tasks              任务集合
	 * @param fragmentIdFunction 获取分片id
	 * @param job                执行任务
	 * @param exceptionHandler   异常处理
	 * @param <T>                任务类
	 */
	<T> void fragment(Collection<T> tasks, FragmentIdFunction<T> fragmentIdFunction, FragmentJob<T> job, ExceptionHandler<T> exceptionHandler);
	
	/**
	 * 是否是当前任务
	 *
	 * @param task               任务
	 * @param fragmentIdFunction 获取分片id
	 * @param <T>                任务类
	 * @return 是否当前线程可以执行任务
	 */
	<T> boolean isCurrentJob(T task, FragmentIdFunction<T> fragmentIdFunction);
}
