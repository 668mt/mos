package mt.spring.mos.server.service.fragment;

import java.util.Collection;

/**
 * @Author Martin
 * @Date 2022/7/14
 */
public interface TaskFragment {
	
	<T> void fragment(Collection<T> tasks, FragmentIdFunction<T> fragmentIdFunction, FragmentJob<T> function);
	
	<T> void fragment(Collection<T> tasks, FragmentIdFunction<T> fragmentIdFunction, FragmentJob<T> function, ExceptionHandler<T> exceptionHandler);
	
	<T> void fragmentByFieldValue(Collection<T> tasks, String fieldName, FragmentJob<T> function);
	
	<T> void fragmentByFieldHashCode(Collection<T> tasks, String fieldName, FragmentJob<T> function);
	
	<T> boolean isCurrentJob(T task, FragmentIdFunction<T> fragmentIdFunction);
}
