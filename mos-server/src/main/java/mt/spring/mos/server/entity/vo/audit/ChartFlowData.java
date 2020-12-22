package mt.spring.mos.server.entity.vo.audit;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2020/12/20
 */
@Data
public class ChartFlowData {
	private String time;
	private double readMb;
	private double writeMb;
}
