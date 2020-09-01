package mt.common.tkmapper;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Mybatis判断
 *
 * @author Martin
 * @ClassName: Filter
 * @Description:
 * @date 2017-10-18 下午2:02:58
 */
@Data
public class Filter {
	public Filter() {
	}
	
	private String property;
	private Object value;
	private Object value2;
	private Operator operator;
	
	/**
	 * 运算符
	 */
	public enum Operator {
		
		/**
		 * 等于
		 */
		eq,
		
		/**
		 * 不等于
		 */
		ne,
		
		/**
		 * 大于
		 */
		gt,
		
		/**
		 * 小于
		 */
		lt,
		
		/**
		 * 大于等于
		 */
		ge,
		
		/**
		 * 小于等于
		 */
		le,
		
		/**
		 * 相似
		 */
		like,
		
		/**
		 * 包含
		 */
		in,
		
		/**
		 * 为Null
		 */
		isNull,
		
		/**
		 * 不为Null
		 */
		isNotNull,
		/**
		 * 等于，如果为null，则判断is null
		 */
		eqn,
		/**
		 * 不在集合里面
		 */
		notIn,
		/**
		 * 自定义条件
		 */
		condition,
		/**
		 * 在之间
		 */
		between,
		/**
		 * 不在之间
		 */
		notBetween,
		/**
		 * 不像
		 */
		notLike
	}
	
	/**
	 * 把运算符转换为sql
	 *
	 * @param operator
	 * @return
	 */
	public static String toSql(Operator operator) {
		if (operator != null) {
			switch (operator) {
				case eq:
					return " = ";
				case eqn:
					throw new RuntimeException("不支持eqn");
				case ge:
					return " >= ";
				case gt:
					return " > ";
				case in:
					return " in ";
				case isNotNull:
					return " is not null ";
				case isNull:
					return " is null ";
				case le:
					return " <= ";
				case like:
					return " like ";
				case lt:
					return " < ";
				case ne:
					return " != ";
				case between:
					return " between ";
				case notBetween:
					return " not between ";
				case notIn:
					return " not in ";
				case notLike:
					return " not like ";
			}
		}
		
		return "";
	}
	
	private static String inValueSql(String paramName, Object value) {
		String sql = " (";
		if (value instanceof Collection) {
			List<String> list = new ArrayList<>();
			Collection c = (Collection) value;
			Iterator iterator = c.iterator();
			int index = 1;
			while (iterator.hasNext()) {
				Object next = iterator.next();
				list.add("#{" + paramName + "_" + index + "}");
				index++;
			}
			sql += StringUtils.join(list, ",");
		} else if (value instanceof Object[]) {
			List<String> list = new ArrayList<>();
			int index = 1;
			Object[] c = (Object[]) value;
			for (Object o : c) {
				list.add("#{" + paramName + "_" + index + "}");
				index++;
			}
			sql += StringUtils.join(list, ",");
		} else {
			sql += value;
		}
		sql += ") ";
		return sql;
	}
	
	public static String toMyBatisSql(@NotNull String paramName, @NotNull Filter filter, @Nullable String alias) {
		Assert.notNull(filter, "参数不能为空");
		Object value2 = filter.getValue2();
		Object value = "#{" + paramName + "}";
		alias = alias == null ? "" : alias + ".";
		String sql = alias + filter.getProperty();
		switch (filter.getOperator()) {
			case eq:
				return sql += " = " + value + " ";
			case eqn:
				return sql += " = " + value + " ";
			case condition:
				if (filter.getValue() != null) {
					return filter.getProperty() + " " + value;
				} else {
					return filter.getProperty();
				}
			case between:
				return sql += " between #{" + paramName + "_1} and #{" + paramName + "_2} ";
			case ge:
				return sql += " >= " + value + " ";
			case gt:
				return sql += " > " + value + " ";
			case in:
				return sql += " in " + inValueSql(paramName, filter.getValue());
			case isNotNull:
				return sql += " is not null ";
			case isNull:
				return sql += " is null ";
			case le:
				return sql += " <= " + value + " ";
			case like:
				return sql += " like " + value + " ";
			case lt:
				return sql += " < " + value + " ";
			case ne:
				return sql += " != " + value + " ";
			case notBetween:
				return sql += " not between #{" + paramName + "_1} and #{" + paramName + "_2} ";
			case notIn:
				return sql += " not in " + inValueSql(paramName, filter.getValue());
			case notLike:
				return sql += " not like " + value + " ";
		}
		
		return sql;
	}
	
	public static String filtersToMybatisSql(@NotNull String parameterMapName, @NotNull List<Filter> filters, @Nullable String alias) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < filters.size(); i++) {
			Filter filter = filters.get(i);
			sb.append(" and ");
			sb.append(toMyBatisSql(parameterMapName + "." + "p" + (i + 1) + "", filter, alias));
		}
		return sb.toString();
	}
	
	/**
	 * 将过滤条件转化为Mybatis参数map
	 *
	 * @param filters
	 * @return
	 */
	public static Map<String, Object> filtersToParameterMap(@NotNull List<Filter> filters) {
		Map<String, Object> parameterMap = new LinkedHashMap<>();
		for (int i = 0; i < filters.size(); i++) {
			Filter filter = filters.get(i);
			Object value = filter.getValue();
			String parameterName = "p" + (i + 1);
			switch (filter.getOperator()) {
				case in:
				case notIn:
					if (value instanceof Collection) {
						Collection collection = (Collection) value;
						Iterator iterator = collection.iterator();
						int index = 1;
						while (iterator.hasNext()) {
							parameterMap.put(parameterName + "_" + index, iterator.next());
							index++;
						}
					} else if (value instanceof Object[]) {
						Object[] array = (Object[]) value;
						int index = 1;
						for (Object o : array) {
							parameterMap.put(parameterName + "_" + index, o);
							index++;
						}
					}
					break;
				case between:
				case notBetween:
					parameterMap.put(parameterName + "_" + 1, filter.getValue());
					parameterMap.put(parameterName + "_" + 2, filter.getValue2());
					break;
				default:
					parameterMap.put(parameterName, value);
					break;
			}
		}
		return parameterMap;
	}
	
	public Filter(String property, Operator operator, Object value) {
		this.property = property;
		this.operator = operator;
		this.value = value;
	}
	
	public Filter(String property, Operator operator) {
		this.property = property;
		this.operator = operator;
	}
	
	public Filter(String property, Operator operator, Object value, Object value2) {
		this.property = property;
		this.value = value;
		this.value2 = value2;
		this.operator = operator;
	}
	
}
