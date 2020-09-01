package mt.common.converter;

import mt.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Date;

@Component
@Slf4j
public class HttpDateConverter implements Converter<String, Date>{
	
	@Override
	public Date convert(String oldDate) {
		try {
			return DateUtils.parse(oldDate);
		} catch (ParseException e) {
			log.error(e.getMessage(),e);
		}
		return null;
	}
	
}
