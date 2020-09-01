package mt.utils;

import org.jetbrains.annotations.NotNull;

/**
 * @Author Martin
 * @Date 2018/11/5
 */
public class ConvertUtils {
	public static <T> T convert(@NotNull Object object, @NotNull Class<T> type) {
		return type.cast(object);
	}
}
