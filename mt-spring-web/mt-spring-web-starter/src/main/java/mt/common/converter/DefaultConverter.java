package mt.common.converter;

public class DefaultConverter implements Converter<Object>{

	@Override
	public Object convert(Object value) {
		return value;
	}

}
