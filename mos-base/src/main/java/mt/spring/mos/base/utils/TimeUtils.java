package mt.spring.mos.base.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @Author Martin
 * @Date 2021/1/9
 */
public class TimeUtils {
	/**
	 * 获取时间
	 * @param millSeconds
	 * @return
	 */
	public static String getReadableTime(long millSeconds) {
		long mills = Math.abs(millSeconds);
		long x;
		String unit;
		if (mills < 1000) {
			x = 1;
			unit = "毫秒";
		} else if (mills < 1000 * 60) {
			x = 1000;
			unit = "秒";
		} else if (mills < 1000 * 60 * 60) {
			x = 1000 * 60;
			unit = "分钟";
		} else if (mills < 1000 * 60 * 60 * 60) {
			x = 1000 * 60 * 60;
			unit = "小时";
		} else {
			x = 1000 * 60 * 60 * 60;
			unit = "天";
		}
		double readableNum = BigDecimal.valueOf(mills).divide(BigDecimal.valueOf(x), 3, RoundingMode.HALF_UP).doubleValue();
		String prefix = millSeconds < 0 ? "-" : "";
		return prefix + readableNum + unit;
	}
}
