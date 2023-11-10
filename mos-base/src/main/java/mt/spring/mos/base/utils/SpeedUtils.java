package mt.spring.mos.base.utils;

import mt.spring.mos.base.utils.SizeUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2023/5/12
 */
public class SpeedUtils {
	/**
	 * 计算速度
	 *
	 * @param length    长度
	 * @param costMills 耗时
	 * @return 速度
	 */
	public static String getSpeed(long length, long costMills) {
		long seconds = TimeUnit.MILLISECONDS.toSeconds(costMills);
		if (seconds <= 0) {
			seconds = 1;
		}
		long speed = BigDecimal.valueOf(length).divide(BigDecimal.valueOf(seconds), 0, RoundingMode.HALF_UP).longValue();
		return SizeUtils.getReadableSize(speed) + "/s";
	}
}
