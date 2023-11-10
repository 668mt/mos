package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.common.annotation.GenerateOrder;
import mt.spring.mos.server.entity.BaseEntity;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Id;

/**
 * @Author Martin
 * @Date 2023/9/9
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class IdBaseEntity extends BaseEntity {
	@Id
	@KeySql(useGeneratedKeys = true)
	@GenerateOrder(6)
	private Long id;
}
