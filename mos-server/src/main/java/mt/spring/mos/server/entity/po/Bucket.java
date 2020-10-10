package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.common.annotation.ForeignKey;
import mt.spring.mos.server.entity.BaseEntity;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

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
	private Long id;
	@Column(unique = true)
	private String bucketName;
	
	@Column(nullable = false)
	@ForeignKey(tableEntity = User.class)
	private Long userId;
	
}
