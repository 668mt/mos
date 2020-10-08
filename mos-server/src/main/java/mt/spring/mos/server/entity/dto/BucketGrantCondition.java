package mt.spring.mos.server.entity.dto;

import lombok.Data;
import mt.common.annotation.Filter;

/**
 * @Author Martin
 * @Date 2020/10/8
 */
@Data
public class BucketGrantCondition {
	@Filter(operator = mt.common.tkmapper.Filter.Operator.eq)
	private Long userId;
	
	@Filter(operator = mt.common.tkmapper.Filter.Operator.eq)
	private Long bucketId;
}
