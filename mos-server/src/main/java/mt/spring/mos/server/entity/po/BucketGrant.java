package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.common.annotation.ForeignKey;
import mt.spring.mos.server.entity.BaseEntity;

import javax.persistence.Id;
import javax.persistence.Table;

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
}
