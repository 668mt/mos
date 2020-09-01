package mt.common.mybatis.utils;

import mt.common.mybatis.exception.NotSupportException;
import mt.common.tkmapper.Filter;
import mt.common.tkmapper.OrFilter;
import mt.utils.MyUtils;
import mt.utils.ReflectUtils;
import org.springframework.util.Assert;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.entity.Example.Criteria;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * mybatis工具类
* @ClassName: MyBatisUtils
* @Description: 
* @author Martin
* @date 2017-10-18 下午1:57:13
*
 */
public class MyBatisUtils {
	
	private MyBatisUtils() {
	}
	
	/**
	 * 创建条件
	 *
	 * @param entityClass
	 * @param filter
	 * @return
	 */
	public static Example createExample(Class<?> entityClass, Filter filter) {
		Assert.notNull(filter);
		List<Filter> filters = new ArrayList<Filter>();
		filters.add(filter);
		return createExample(entityClass, filters);
	}
	
	private static Map<Class<?>, List<String>> entityFields = new ConcurrentHashMap<>();
	
	/**
	 * filter兼容忽略大小写、驼峰转换
	 *
	 * @param entityClass
	 * @param property
	 * @return
	 */
	private static String getField(Class<?> entityClass, String property) {
		List<String> list = entityFields.get(entityClass);
		if (list == null) {
			List<Field> allFields = ReflectUtils.findAllFields(entityClass);
			list = new ArrayList<>();
			if (MyUtils.isNotEmpty(allFields)) {
				for (Field allField : allFields) {
					list.add(allField.getName());
				}
			}
			entityFields.put(entityClass, list);
		}
		for (String s : list) {
			if (s.equals(property)) {
				return s;
			}
		}
		for (String s : list) {
			if (s.equalsIgnoreCase(property)) {
				return s;
			}
		}
		//驼峰转换一致
		String parsedColumn = MapperColumnUtils.parseColumn(property);
		for (String s : list) {
			if (MapperColumnUtils.parseColumn(s).equalsIgnoreCase(parsedColumn)) {
				return s;
			}
		}
		return property;
	}
	
	/**
	 * 创建条件
	 *
	 * @param entityClass
	 * @param filters
	 * @return
	 */
	public static Example createExample(Class<?> entityClass, List<Filter> filters) {
		Example example = new Example(entityClass);
		if (MyUtils.isNotEmpty(filters)) {
			for (Filter filterInterface : filters) {
				if (filterInterface instanceof OrFilter) {
					OrFilter orFilter = (OrFilter) filterInterface;
					Filter[] andFilters = orFilter.getFilters();
					Criteria criteria = example.and();
					for (Filter filter : andFilters) {
						Filter.Operator operator = filter.getOperator();
						String property = getField(entityClass, filter.getProperty());
						Object value = filter.getValue();
						Object value2 = filter.getValue2();
						switch (operator) {
							case eq:
								criteria.orEqualTo(property, value);
								break;
							case gt:
								criteria.orGreaterThan(property, value);
								break;
							case lt:
								criteria.orLessThan(property, value);
								break;
							case ge:
								criteria.orGreaterThanOrEqualTo(property, value);
								break;
							case in:
								if (value instanceof Object[]) {
									criteria.orIn(property, MyUtils.toList((Object[]) value));
								} else if (value instanceof Iterable) {
									criteria.orIn(property, (Iterable<?>) value);
								} else {
									throw new NotSupportException("in 不支持传入非数组或非集合类型");
								}
								break;
							case isNotNull:
								criteria.orIsNotNull(property);
								break;
							case isNull:
								criteria.orIsNull(property);
								break;
							case le:
								criteria.orLessThanOrEqualTo(property, value);
								break;
							case like:
								criteria.orLike(property, value + "");
								break;
							case ne:
								criteria.orNotEqualTo(property, value);
								break;
							case eqn:
								if (value == null) {
									criteria.orIsNull(property);
								} else {
									criteria.orEqualTo(property, value);
								}
								break;
							case notIn:
								if (value instanceof Object[]) {
									criteria.orNotIn(property, MyUtils.toList((Object[]) value));
								} else if (value instanceof Iterable) {
									criteria.orNotIn(property, (Iterable<?>) value);
								} else {
									throw new NotSupportException("in 不支持传入非数组或非集合类型");
								}
								break;
							case condition:
								if (value != null) {
									criteria.orCondition(property, value);
								} else {
									criteria.orCondition(property);
								}
								break;
							case between:
								criteria.orBetween(property, value, value2);
								break;
							case notBetween:
								criteria.orNotBetween(property, value, value2);
								break;
							case notLike:
								criteria.orNotLike(property, value + "");
								break;
						}
					}
				} else {
					Filter.Operator operator = filterInterface.getOperator();
					String property = getField(entityClass, filterInterface.getProperty());
					Object value = filterInterface.getValue();
					Object value2 = filterInterface.getValue2();
					switch (operator) {
						case eq:
							example.and().andEqualTo(property, value);
							break;
						case gt:
							example.and().andGreaterThan(property, value);
							break;
						case lt:
							example.and().andLessThan(property, value);
							break;
						case ge:
							example.and().andGreaterThanOrEqualTo(property, value);
							break;
						case in:
							if (value instanceof Object[]) {
								example.and().andIn(property, MyUtils.toList((Object[]) value));
							} else if (value instanceof Iterable) {
								example.and().andIn(property, (Iterable<?>) value);
							} else {
								throw new NotSupportException("in 不支持传入非数组或非集合类型");
							}
							break;
						case isNotNull:
							example.and().andIsNotNull(property);
							break;
						case isNull:
							example.and().andIsNull(property);
							break;
						case le:
							example.and().andLessThanOrEqualTo(property, value);
							break;
						case like:
							example.and().andLike(property, value + "");
							break;
						case ne:
							example.and().andNotEqualTo(property, value);
							break;
						case eqn:
							if (value == null) {
								example.and().andIsNull(property);
							} else {
								example.and().andEqualTo(property, value);
							}
							break;
						case notIn:
							if (value instanceof Object[]) {
								example.and().andNotIn(property, MyUtils.toList((Object[]) value));
							} else if (value instanceof Iterable) {
								example.and().andNotIn(property, (Iterable<?>) value);
							} else {
								throw new NotSupportException("in 不支持传入非数组或非集合类型");
							}
							break;
						case condition:
							if (value != null) {
								example.and().andCondition(property, value);
							} else {
								example.and().andCondition(property);
							}
							break;
						case between:
							example.and().andBetween(property, value, value2);
							break;
						case notBetween:
							example.and().andNotBetween(property, value, value2);
							break;
						case notLike:
							example.and().andNotLike(property, value + "");
							break;
					}
				}
			}
		}
		return example;
	}
}
