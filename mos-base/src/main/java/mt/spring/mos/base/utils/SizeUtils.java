package mt.spring.mos.base.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * @Author Martin
 * @Date 2020/5/22
 */
public class SizeUtils {
	public static String getReadableSize(long sizeByte) {
		BigDecimal size;
		String unit;
		if (sizeByte < 0) {
			return null;
		} else if (sizeByte <= 1024) {
			size = BigDecimal.valueOf(sizeByte);
			unit = "B";
		} else if (sizeByte <= 1024 * 1024) {
			size = BigDecimal.valueOf(sizeByte).divide(BigDecimal.valueOf(1024), 3, RoundingMode.HALF_UP);
			unit = "KB";
		} else if (sizeByte <= 1024 * 1024 * 1024) {
			size = BigDecimal.valueOf(sizeByte).divide(BigDecimal.valueOf(1024 * 1024), 3, RoundingMode.HALF_UP);
			unit = "MB";
		} else {
			size = BigDecimal.valueOf(sizeByte).divide(BigDecimal.valueOf(1024 * 1024 * 1024), 3, RoundingMode.HALF_UP);
			unit = "GB";
		}
		return new DecimalFormat("#.###").format(size) + unit;
	}
}
