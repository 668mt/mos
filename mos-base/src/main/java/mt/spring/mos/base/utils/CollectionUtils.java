package mt.spring.mos.base.utils;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @Author Martin
 * @Date 2021/1/9
 */
public class CollectionUtils {
	public static boolean isEmpty(@Nullable Collection<?> collection) {
		return collection == null || collection.size() == 0;
	}
	
	public static boolean isEmpty(@Nullable Object[] array) {
		return array == null || array.length == 0;
	}
	
	public static boolean isNotEmpty(@Nullable Collection<?> collection) {
		return !isEmpty(collection);
	}
	
	public static boolean isNotEmpty(@Nullable Object[] array) {
		return !isEmpty(array);
	}
}
