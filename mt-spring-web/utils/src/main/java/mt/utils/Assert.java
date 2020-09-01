package mt.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;

/**
 * @Author Martin
 * @Date 2019/12/8
 */
public class Assert {
	public static void state(boolean state) {
		state(state, null);
	}
	
	public static void notNull(Object o) {
		notNull(o, null);
	}
	
	public static void notBlank(String content) {
		notBlank(content, null);
	}
	
	public static void notEmpty(Collection c) {
		notEmpty(c, null);
	}
	
	public static void notEmpty(Object[] arr) {
		notEmpty(arr, null);
	}
	
	public static void state(boolean state, String errorMsg) {
		if (!state) {
			if (errorMsg != null) {
				throw new IllegalStateException(errorMsg);
			} else {
				throw new IllegalStateException();
			}
		}
	}
	
	public static void notNull(Object o, String errorMsg) {
		if (o == null) {
			if (errorMsg != null) {
				throw new IllegalArgumentException(errorMsg);
			} else {
				throw new IllegalArgumentException();
			}
		}
	}
	
	/**
	 * Assert that an object is not {@code null}.
	 * <pre class="code">Assert.notNull(clazz, "The class must not be null");</pre>
	 *
	 * @param content  the object to check
	 * @param errorMsg the exception message to use if the assertion fails
	 * @throws IllegalArgumentException if the object is {@code null}
	 */
	public static void notBlank(String content, String errorMsg) {
		if (StringUtils.isBlank(content)) {
			if (errorMsg != null) {
				throw new IllegalArgumentException(errorMsg);
			} else {
				throw new IllegalArgumentException();
			}
		}
	}
	
	public static void notEmpty(Collection c, String errorMsg) {
		if (MyUtils.isEmpty(c)) {
			if (errorMsg != null) {
				throw new IllegalArgumentException(errorMsg);
			} else {
				throw new IllegalArgumentException();
			}
		}
	}
	
	public static void notEmpty(Object[] arr, String errorMsg) {
		if (MyUtils.isEmpty(arr)) {
			if (errorMsg != null) {
				throw new IllegalArgumentException(errorMsg);
			} else {
				throw new IllegalArgumentException();
			}
		}
	}
}
