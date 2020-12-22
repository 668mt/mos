package mt.spring.mos.server.entity.vo.audit;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author Martin
 * @Date 2020/12/20
 */
@Data
public class ChartVo {
	private String x;
	private BigDecimal y;
	
	public ChartVo(String x, BigDecimal y) {
		this.x = x;
		this.y = y;
	}
	
	public ChartVo() {
	}
}
