package mt.spring.mos.base.utils;

import java.lang.reflect.Field;

/**
 * @Author Martin
 * @Date 2020/11/25
 */
public class ReflectUtils {
	public static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
		try {
			return clazz.getDeclaredField(name);
		} catch (NoSuchFieldException e) {
			Class<?> superclass = clazz.getSuperclass();
			if (superclass.equals(Object.class)) {
				throw e;
			}
			return findField(superclass, name);
		}
	}
	
	public static Object getValue(Object obj, String name) throws NoSuchFieldException {
		Field field = findField(obj.getClass(), name);
		field.setAccessible(true);
		try {
			return field.get(obj);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
