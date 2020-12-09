package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.common.annotation.ForeignKey;
import mt.spring.mos.server.entity.BaseEntity;
import mt.spring.mos.server.entity.BucketPerm;
import mt.spring.mos.server.entity.handler.PermsTypeHandler;
import tk.mybatis.mapper.annotation.ColumnType;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.List;

/**
 * @Author Martin
 * @Date 2020/10/8
 */
@Table(name = "mos_bucket_grant")
@Data
@EqualsAndHashCode(callSuper = false)
public class BucketGrant extends BaseEntity {
	@ForeignKey(tableEntity = Bucket.class)
	@Id
	private Long bucketId;
	
	@ForeignKey(tableEntity = User.class)
	@Id
	private Long userId;
	
	@ColumnType(typeHandler = PermsTypeHandler.class)
	@Column(length = 200)
	private List<BucketPerm> perms;
}
