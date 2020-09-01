package mt.common.converter;

/**
 * @Author Martin
 * @Date 2018/11/6
 */
public class BooleanConverter implements Converter<Boolean> {
	@Override
	public Object convert(Boolean value) {
		if (value == null)
			return null;
		return value ? 1 : 0;
	}
}
