package mt.common.converter;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateConverter implements Converter<Date>{
	private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	@Override
	public String convert(Date value) {
		if(value != null){
			return format.format(value);
		}
		return null;
	}

}
