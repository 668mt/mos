package mt.spring.mos.server.entity.vo.audit;

import lombok.Data;
import mt.spring.mos.base.utils.SizeUtils;

/**
 * @Author Martin
 * @Date 2020/12/20
 */
@Data
public class StatisticInfo {
	private long thisDayReadBytes;
	private long thisDayWriteBytes;
	private long thisDayReadRequests;
	private long thisDayWriteRequests;
	
	private long thisMonthReadBytes;
	private long thisMonthWriteBytes;
	private long thisMonthReadRequests;
	private long thisMonthWriteRequests;
	
	public String getReadableThisDayRead() {
		return SizeUtils.getReadableSize(thisDayReadBytes);
	}
	
	public String getReadableThisDayWrite() {
		return SizeUtils.getReadableSize(thisDayWriteBytes);
	}
	
	public String getReadableThisMonthRead() {
		return SizeUtils.getReadableSize(thisMonthReadBytes);
	}
	
	public String getReadableThisMonthWrite() {
		return SizeUtils.getReadableSize(thisMonthWriteBytes);
	}
}
