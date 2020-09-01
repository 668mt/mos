package mt.utils;

import mt.utils.http.MyHttp;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Martin
 * @ClassName: MyUtils
 * @Description:
 * @date 2017-10-13 下午3:38:52
 */
public class MyUtils {
	private static Properties properties;
	/**
	 * 默认数据库max最大字符串长度
	 */
	public static final int DEFAULT_MAX_LENGTH = 8000;
	
	/**
	 * alertqq表中content1最大长度
	 */
	public static final int ALERTQQ_CONTENT1_MAX_LENGTH = 4000;
	
	/**
	 * 特殊字符正则表达式
	 */
	public static final String SPECIAL_CHAR_REGEX = "[\n\\s\"`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？ ]";
	/**
	 * 特殊字符
	 */
	public static final String SPECIAL_CHAR = "\"`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？ ";
	
	/**
	 * 从配置文件里面读取参数
	 *
	 * @param key 参数名
	 * @return
	 */
	public synchronized static String getParamFromProp(String key) {
		return getParamFromProp(key, String.class);
	}
	
	public synchronized static <T> T getParamFromProp(String key, Class<T> type) {
		return getParamFromProp(key, type, null);
	}
	
	@SuppressWarnings("unchecked")
	public synchronized static <T> T getParamFromProp(String key, Class<T> type, String filePath) {
		if (StringUtils.isBlank(filePath)) {
			filePath = "application.properties";
		}
		if (properties == null) {
			try {
				InputStream is = MyUtils.class.getClassLoader().getResourceAsStream(filePath);
				if (is == null) {
					return null;
				}
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				Properties props = new Properties();
				props.load(br);
				properties = props;
				if (filePath.equals("application.properties")) {
					String active = (String) props.get("spring.profiles.active");
					if (StringUtils.isNotBlank(active)) {
						InputStream is2 = MyUtils.class.getClassLoader().getResourceAsStream("application-" + active.trim() + ".properties");
						if (is2 != null) {
							BufferedReader br2 = new BufferedReader(new InputStreamReader(is2));
							Properties props2 = new Properties();
							props2.load(br2);
							properties = props2;
						}
					}
				}
				return (T) ConvertUtils.convert(properties.get(key), type);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return (T) ConvertUtils.convert(properties.get(key), type);
	}
	
	/**
	 * 判断字符串是否超过指定字节，如果超过则进行截取
	 *
	 * @param message
	 * @param max
	 * @return
	 */
	public static String substring(String message, int max) {
		try {
			if (message != null) {
				int len = message.getBytes("GB2312").length;
				if (len > max) {
					//进行截取
					byte[] bytes = message.getBytes("GB2312");
					return new String(ArrayUtils.subarray(bytes, 0, max), "GB2312");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return message;
	}
	
	/**
	 * 发送qq消息，成功返回 "success"
	 *
	 * @param message 消息
	 * @return
	 */
	public static String sendMessage(String message) {
		try {
			message = substring(message, ALERTQQ_CONTENT1_MAX_LENGTH);
			MyHttp myHttp = new MyHttp(getParamFromProp("sync.url") + "/exception/send");
			myHttp.setMethod("post");
			myHttp.setParam(message);
			return myHttp.connect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 发送qq消息，成功返回 "success"
	 *
	 * @param e 异常消息
	 * @return
	 */
	public static String sendMessage(Throwable e) {
		return sendMessage(getExceptionMessage(e));
	}
	
	/**
	 * 获取异常信息
	 *
	 * @param e 异常
	 * @return
	 */
	public static String getExceptionMessage(Throwable e) {
		return getExceptionMessage(e, DEFAULT_MAX_LENGTH);
	}
	
	/**
	 * 获取异常信息
	 *
	 * @param e         异常
	 * @param maxLength 最大长度
	 * @return
	 */
	public static String getExceptionMessage(Throwable e, Integer maxLength) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		e.printStackTrace(new PrintStream(bos));
		byte[] buffer = bos.toByteArray();
		String exception = new String(buffer);
		return substring(exception, maxLength);
	}
//	/**
//	 * 拷贝对象属性
//	 * @param <T>
//	 *
//	 * @param source
//	 *            源
//	 * @param target
//	 *            目标
//	 * @param ignoreProperties
//	 *            忽略属性
//	 */
//	public static <T> void copyProperties(T source, T target, String... ignoreProperties) {
//		Assert.notNull(source);
//		Assert.notNull(target);
//
//		PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(target);
//		for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
//			String propertyName = propertyDescriptor.getName();
//			Method readMethod = propertyDescriptor.getReadMethod();
//			Method writeMethod = propertyDescriptor.getWriteMethod();
//			if (ArrayUtils.contains(ignoreProperties, propertyName) || readMethod == null || writeMethod == null) {
//				continue;
//			}
//			try {
//				Object sourceValue = readMethod.invoke(source);
//				writeMethod.invoke(target, sourceValue);
//			} catch (IllegalAccessException e) {
//				throw new RuntimeException(e.getMessage(), e);
//			} catch (IllegalArgumentException e) {
//				throw new RuntimeException(e.getMessage(), e);
//			} catch (InvocationTargetException e) {
//				throw new RuntimeException(e.getMessage(), e);
//			}
//		}
//	}
	
	/**
	 * 判断集合是否为空
	 *
	 * @param collection
	 * @return
	 */
	public static boolean isEmpty(Collection<?> collection) {
		if (collection == null || collection.size() == 0) {
			return true;
		}
		return false;
	}
	
	/**
	 * 判断集合是否不为空
	 *
	 * @param collection
	 * @return
	 */
	public static boolean isNotEmpty(Collection<?> collection) {
		return !isEmpty(collection);
	}
	
	/**
	 * 判断数组是否为空
	 *
	 * @param arr
	 * @return
	 */
	public static boolean isEmpty(Object[] arr) {
		if (arr == null || arr.length == 0) {
			return true;
		}
		return false;
	}
	
	/**
	 * 判断数组不为空
	 *
	 * @param arr
	 * @return
	 */
	public static boolean isNotEmpty(Object[] arr) {
		return !isEmpty(arr);
	}
	
	/**
	 * 将数组转换成List，数组为空时返回空list
	 *
	 * @param arr
	 * @return
	 */
	public static <E> List<E> toList(E[] arr) {
		List<E> list = new ArrayList<E>();
		if (isNotEmpty(arr)) {
			for (E object : arr) {
				list.add(object);
			}
		}
		return list;
	}
	
	public static <E> Set<E> toSet(E[] arr) {
		Set<E> list = new HashSet<E>();
		if (isNotEmpty(arr)) {
			for (E object : arr) {
				list.add(object);
			}
		}
		return list;
	}
	
	/**
	 * 如果字符串为Null 返回空串
	 *
	 * @param str
	 * @return
	 */
	public static String blank(String str) {
		if (str == null) {
			return "";
		}
		return str;
	}
	
	public static String dateFormat(String date, String pattern) throws ParseException {
		if (StringUtils.isBlank(date)) {
			return "";
		}
		return new SimpleDateFormat(pattern).format(DateUtils.parse(date));
	}
	
	public static String toFixed(String number, int len) {
		return toFixed(number, len, false);
	}
	
	public static String toFixed(String number, int len, boolean blankSetZero) {
		double value;
		if (StringUtils.isBlank(number)) {
			if (blankSetZero) {
				value = 0;
			} else {
				return "";
			}
		} else {
			value = new BigDecimal(number).setScale(len, RoundingMode.HALF_UP).doubleValue();
		}
		String pattern = "#";
		if (len > 0) {
			pattern += ".";
			for (int i = 0; i < len; i++) {
				pattern += "0";
			}
		}
		String format = new DecimalFormat(pattern).format(value);
		return format;
	}
	
	/**
	 * 进行List分组
	 *
	 * @param list 需要分组的列表
	 * @param key  按指定字段进行分组
	 * @param type 指定字段的数据类型
	 * @return
	 */
	public static <T, T2> Map<T2, List<T>> groupBy(List<T> list, String key, Class<T2> type) {
		Map<T2, List<T>> map = new HashMap<T2, List<T>>();
		for (T obj : list) {
			T2 value = ReflectUtils.getValue(obj, key, type);
			List<T> list2 = map.get(value);
			if (isEmpty(list2)) {
				list2 = new ArrayList<T>();
				map.put(value, list2);
			}
			list2.add(obj);
		}
		return map;
	}
	
	/**
	 * 按字符串字段分组
	 *
	 * @param list 待分组的List
	 * @param key  按指定字段进行分组
	 * @return
	 */
	public static <T> Map<String, List<T>> groupBy(List<T> list, String key) {
		return groupBy(list, key, String.class);
	}
	
	public static <T> T nullAsDefault(T value, T... defaultValues) {
		if (value != null)
			return value;
		if (defaultValues == null)
			return null;
		for (T arg : defaultValues) {
			if (arg != null)
				return arg;
		}
		return null;
	}
}
