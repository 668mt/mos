package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.common.annotation.ForeignKey;
import mt.generator.mybatis.annotation.Index;
import mt.generator.mybatis.annotation.Indexs;
import mt.spring.mos.server.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @Author Martin
 * @Date 2020/12/19
 */
@Data
@Table(name = "mos_audit_archive")
@EqualsAndHashCode(callSuper = false)
@Indexs({
		@Index(columns = "userId"),
		@Index(columns = "openId"),
		@Index(columns = "type"),
		@Index(columns = "action")
})
public class AuditArchive extends BaseEntity {
	@Id
	private Long id;
	
	@ForeignKey(tableEntity = Bucket.class, casecadeType = ForeignKey.CascadeType.ALL)
	private Long bucketId;
	
	private Long userId;
	
	private Long openId;
	
	@Column(columnDefinition = "text")
	private String target;
	
	private Audit.Type type;
	
	private Long bytes;
	
	private Audit.Action action;
	
	@Column(columnDefinition = "text")
	private String remark;
	
	private String ip;
}
