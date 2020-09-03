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
 * @Date 2020/5/20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "mos_access_control")
public class AccessControl extends BaseEntity {
	private static final long serialVersionUID = -1377191126823875077L;
	
	@Id
	@KeySql(useGeneratedKeys = true)
	private Long openId;
	@Column(nullable = false, columnDefinition = "text")
	private String publicKey;
	@Column(nullable = false, columnDefinition = "text")
	private String privateKey;
	@ForeignKey(tableEntity = Bucket.class, casecadeType = ForeignKey.CascadeType.ALL)
	private Long bucketId;
}
