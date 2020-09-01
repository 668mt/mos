package mt.utils;

import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {
	/**
	 * 把字符串的日期格式转化为日期类型
	 * @param text
	 * @return
	 * @throws ParseException
	 */
	public static Date parse(String text) throws ParseException {
		if (StringUtils.isBlank(text)) {
			return null;
		}
		String pattern = getPattern(text);
		if(pattern != null){
			return new SimpleDateFormat(pattern).parse(text);
		}
		return new Date(text);
	}
	public static String getPattern(String text){
		if (text.matches("\\b\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d\\b")) {
			return "yyyy-MM-dd HH:mm:ss.S";
		}else if (text.matches("\\b\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{7}\\b")) {
			return "yyyy-MM-dd HH:mm:ss.SSSSSSS";
		} else if (text.matches("\\b\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}:\\d{3}\\b")) {
			return "yyyy-MM-dd HH:mm:ss:SSS";
		} else if (text.matches("\\b\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\b")) {
			return "yyyy-MM-dd HH:mm:ss.SSS";
		} else if (text.matches("\\b\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\b")) {
			return "yyyy-MM-dd HH:mm:ss";
		} else if (text.matches("\\b\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}\\b")) {
			return "yyyy-MM-dd HH:mm";
		} else if (text.matches("\\b\\d{4}-\\d{2}-\\d{2}\\b")) {
			return "yyyy-MM-dd";
		} else if (text.matches("\\b\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d\\b")) {
			return "yyyy/MM/dd HH:mm:ss.S";
		} else if (text.matches("\\b\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}:\\d{3}\\b")) {
			return "yyyy/MM/dd HH:mm:ss:SSS";
		} else if (text.matches("\\b\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\b")) {
			return "yyyy/MM/dd HH:mm:ss.SSS";
		} else if (text.matches("\\b\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}\\b")) {
			return "yyyy/MM/dd HH:mm:ss";
		} else if (text.matches("\\b\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}\\b")) {
			return "yyyy/MM/dd HH:mm";
		} else if (text.matches("\\b\\d{4}-\\d{2}-\\d{2}\\b")) {
			return "yyyy/MM/dd";
		}
		return null;
	}
}
