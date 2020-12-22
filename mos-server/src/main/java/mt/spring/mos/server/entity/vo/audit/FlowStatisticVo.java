package mt.spring.mos.server.entity.vo.audit;

import lombok.Data;
import mt.spring.mos.server.entity.po.Audit;

/**
 * @Author Martin
 * @Date 2020/12/20
 */
@Data
public class FlowStatisticVo {
	private String bucketName;
	private String startDate;
	private Audit.Type type;
	private String readableFlow;
}
