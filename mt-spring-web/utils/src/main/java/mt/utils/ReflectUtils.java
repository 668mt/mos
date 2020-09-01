package mt.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

/**
 * 反射工具类
 *
 * @ClassName: ReflectUtils
 * @Description:
 * @Author LIMAOTAO236
 * @date 2017-9-28 下午6:18:29
 */
@Slf4j
public class ReflectUtils {
	
	private static final Map<String, List<Field>> caches = new HashMap<>();
	
	private void addCache(String name, List<Field> list) {
		if (caches.size() >= 100)
			caches.clear();
		caches.put(name, list);
	}
	
	private void removeCache(String name) {
		caches.remove(name);
	}
	
	/**
	 * 通过注解查找字段
	 *
	 * @param class1          对象类
	 * @param annotationClass
	 * @return
	 */
	public static List<Field> findFields(Class<?> class1, Class<? extends Annotation> annotationClass) {
		List<Field> list = new ArrayList<>();
		Field[] fields = class1.getDeclaredFields();
		for (Field field : fields) {
			Annotation annotation = AnnotatedElementUtils.findMergedAnnotation(field, annotationClass);
			if (annotation != null) {
				list.add(field);
			}
		}
		return list;
	}
	
	/**
	 * @param class1
	 * @param annotationClass
	 * @return
	 */
	public static Field findField(Class<?> class1, Class<? extends Annotation> annotationClass) {
		List<Field> findFields = findFields(class1, annotationClass);
		if (findFields.size() > 0) {
			return findFields.get(0);
		}
		return null;
	}
	
	public static <T> List<T> findAllValues(Object obj, Class<T> type) {
		List<T> values = new ArrayList<>();
		if (obj == null) {
			return values;
		}
		List<Field> allFields = findAllFields(obj.getClass());
		if (MyUtils.isEmpty(allFields)) {
			return values;
		}
		allFields.forEach(field -> {
			if (type.isAssignableFrom(field.getType())) {
				field.setAccessible(true);
				try {
					Object o = field.get(obj);
					if (o != null) {
						values.add((T) o);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
		return values;
	}
	
	/**
	 * 查询本类和所有父类中所有的字段
	 *
	 * @param class1
	 * @return
	 */
	public static List<Field> findAllFields(Class<?> class1) {
		if (class1 == null) return new ArrayList<>();
		String className = class1.getName();
		List<Field> cacheResults = caches.get(className);
		if (cacheResults != null) {
			return cacheResults;
		}
		List<Field> fieldList = new ArrayList<>();
		List<Class<?>> classList = new ArrayList<>();
		while (class1 != null) {//当父类为null的时候说明到达了最上层的父类(Object类).
			classList.add(class1);
			class1 = class1.getSuperclass(); //得到父类,然后赋给自己
		}
		if (MyUtils.isNotEmpty(classList)) {
			for (int i = 0; i < classList.size(); i++) {
				Class<?> class2 = classList.get(i);
				fieldList.addAll(Arrays.asList(class2.getDeclaredFields()));
			}
		}
		
		//去掉重复的字段
		List<Field> fields = new ArrayList<>();
		for (Field field : fieldList) {
			if (!contains(fields, field)) {
				fields.add(field);
			}
		}
		caches.put(className, fields);
		return fields;
	}
	
	public static boolean contains(List<Field> fields, Field field) {
		if (fields != null) {
			for (Field f : fields) {
				if (f.getName().equals(field.getName())) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * 找本类和所有父类所有 不带有固有注解的字段
	 *
	 * @param class1
	 * @param ignoreAnnotationClass 过滤注解
	 * @return
	 */
	public static List<Field> findAllFieldsIgnore(Class<?> class1, final Class<? extends Annotation> ignoreAnnotationClass) {
		if (ignoreAnnotationClass != null) {
			return ignore(findAllFields(class1), ignoreAnnotationClass);
		}
		return findAllFields(class1);
	}
	
	/**
	 * 过滤字段
	 *
	 * @param listFields
	 * @param ignoreAnnotationClass 使用过滤的注解
	 * @return 不包含该注解的所有字段
	 */
	public static List<Field> ignore(List<Field> listFields, final Class<? extends Annotation> ignoreAnnotationClass) {
		List<Field> result = new ArrayList<Field>();
		CollectionUtils.select(listFields, new Predicate() {
			@Override
			public boolean evaluate(Object obj) {
				Field field = (Field) obj;
				return AnnotatedElementUtils.findMergedAnnotation(field, ignoreAnnotationClass) == null;
			}
		}, result);
		return result;
	}
	
	/**
	 * 过滤字段
	 *
	 * @param listFields
	 * @param annotationClass 使用过滤的注解
	 * @return 包含该注解的所有字段
	 */
	public static List<Field> select(List<Field> listFields, final Class<? extends Annotation> annotationClass) {
		List<Field> result = new ArrayList<>();
		CollectionUtils.select(listFields, obj -> {
			Field field = (Field) obj;
			return AnnotatedElementUtils.findMergedAnnotation(field, annotationClass) == null;
		}, result);
		return result;
	}
	
	/**
	 * 找本类和所有父类所有带有固有注解的字段
	 *
	 * @param class1
	 * @param annotationClass
	 * @return
	 */
	public static List<Field> findAllFields(Class<?> class1, Class<? extends Annotation> annotationClass) {
		List<Field> list = new ArrayList<>();
		if (annotationClass != null) {
			List<Field> findAllFields = findAllFields(class1);
			for (Field field : findAllFields) {
				if (AnnotatedElementUtils.findMergedAnnotation(field, annotationClass) != null) {
					list.add(field);
				}
			}
		}
		return list;
	}
	
	public static Field findOnlyField(Class<?> class1, Class<? extends Annotation> annotationClass) {
		if (annotationClass != null) {
			List<Field> findAllFields = findAllFields(class1);
			for (Field field : findAllFields) {
				if (AnnotatedElementUtils.findMergedAnnotation(field, annotationClass) != null) {
					return field;
				}
			}
		}
		return null;
	}
	
	/**
	 * 从子类往父类查找字段，如果有多个，返回第一个
	 *
	 * @param class1
	 * @param fieldName 字段名
	 */
	public static Field findField(Class<?> class1, final String fieldName) {
		List<Field> findAllFields = findAllFields(class1);
		List<Field> list = new ArrayList<>();
		CollectionUtils.select(findAllFields, object -> {
			Field field = (Field) object;
			return field.getName().equals(fieldName);
		}, list);
		if (list.size() > 0) {
			return list.get(0);
		}
		return null;
	}
	
	public static @Nullable
	Field findFieldWithPath(Class<?> clazz, String path) {
		String[] names = path.split("\\.");
		Class<?> srcClass = clazz;
		Field finalField = null;
		for (String name : names) {
			finalField = findField(srcClass, name);
			if (finalField == null) {
				return null;
			}
			srcClass = finalField.getType();
		}
		return finalField;
	}
	
	/**
	 * 获取值
	 *
	 * @param obj  对象
	 * @param name xx.xx.xx
	 * @param type 类型
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getValue(Object obj, String name, Class<T> type) {
		if (obj == null) {
			return null;
		}
		while (name.contains(".")) {
			int indexOf = name.indexOf(".");
			String field = name.substring(0, indexOf);
			name = name.substring(indexOf + 1, name.length());
			obj = getValue(obj, field, Object.class);
		}
		Field field = findField(obj.getClass(), name);
		if (field != null) {
			try {
				field.setAccessible(true);
				return (T) field.get(obj);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				field.setAccessible(false);
			}
		}
		return null;
	}
	
	public static void setValue(Object obj, String field, Object value) {
		Field field1 = findField(obj.getClass(), field);
		if (field1 == null)
			return;
		try {
			field1.setAccessible(true);
			field1.set(obj, value);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} finally {
			field1.setAccessible(false);
		}
	}
	
	public static void setValueWithPath(Object obj, String path, Object value) {
		List<String[]> list = RegexUtils.findList(path, "-([a-z])", new Integer[]{0, 1});
		if (MyUtils.isNotEmpty(list)) {
			for (String[] s : list) {
				path = path.replace(s[0], s[1].toUpperCase());
			}
		}
		String[] names = path.split("\\.");
		Class<?> srcClass = obj.getClass();
		Object srcObj = obj;
		Field finalField = null;
		for (int i = 0; i < names.length; i++) {
			String name = names[i];
			finalField = findField(srcClass, name);
			if (finalField == null) {
				log.warn(obj.getClass().getName() + "中找不到" + path + "的字段：" + name);
				return;
			}
			try {
				finalField.setAccessible(true);
				Object findObj = finalField.get(srcObj);
				if (findObj == null) {
					try {
						findObj = finalField.getType().newInstance();
						finalField.set(srcObj, findObj);
					} catch (Exception ignore) {
					}
				}
				if (i < names.length - 1) {
					srcObj = findObj;
					srcClass = srcObj.getClass();
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				finalField.setAccessible(false);
			}
		}
		
		if (finalField != null) {
			try {
				finalField.setAccessible(true);
				finalField.set(srcObj, ConvertUtils.convert(value, finalField.getType()));
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} finally {
				finalField.setAccessible(false);
			}
		}
	}
	
	public static boolean hasFieldNameIgnoreCase(Class<?> class1, String fieldName) {
		List<Field> allFields = findAllFields(class1);
		if (MyUtils.isNotEmpty(allFields)) {
			for (Field field : allFields) {
				if (field.getName().equalsIgnoreCase(fieldName)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean hasFieldName(Class<?> class1, String fieldName) {
		List<Field> allFields = findAllFields(class1);
		if (MyUtils.isNotEmpty(allFields)) {
			for (Field field : allFields) {
				if (field.getName().equals(fieldName)) {
					return true;
				}
			}
		}
		return false;
	}
}
