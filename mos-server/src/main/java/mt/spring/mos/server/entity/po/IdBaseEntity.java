package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.common.annotation.GenerateOrder;
import mt.spring.mos.server.entity.BaseEntity;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
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
	// 主键由数据库生成，不参与 INSERT：PG 下显式插入 NULL 会违反非空约束
	@Column(insertable = false)
	@GenerateOrder(6)
	private Long id;
}
