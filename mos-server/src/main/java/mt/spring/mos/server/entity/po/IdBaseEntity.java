package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
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
	private Long id;
}
