package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.common.annotation.ForeignKey;
import mt.spring.mos.server.entity.BaseEntity;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @Author Martin
 * @Date 2020/5/23
 */
@Table(name = "mos_bucket")
@Data
@EqualsAndHashCode(callSuper = false)
public class Bucket extends BaseEntity {
	private static final long serialVersionUID = 6019395507354197069L;
	@Id
	@KeySql(useGeneratedKeys = true)
	// 主键由数据库生成，不参与 INSERT：PG 下显式插入 NULL 会违反非空约束
	@Column(insertable = false)
	private Long id;
	@Column(unique = true, nullable = false)
	private String bucketName;
	private Boolean defaultIsPublic;
	
	@Column(nullable = false)
	@ForeignKey(tableEntity = User.class)
	private Long userId;
	
	private Integer dataFragmentsAmount;
	
}
