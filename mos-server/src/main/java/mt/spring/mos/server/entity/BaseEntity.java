package mt.spring.mos.server.entity;

import lombok.Data;
import mt.common.annotation.CreatedBy;
import mt.common.annotation.CreatedDate;
import mt.common.annotation.LastModifiedBy;
import mt.common.annotation.LastModifiedDate;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author Martin
 * @Date 2020/5/16
 */
@Data
public class BaseEntity implements Serializable {
	private static final long serialVersionUID = -1294407818709225639L;
	@CreatedDate
	private Date createdDate;
	@CreatedBy
	private Long createdBy;
	@LastModifiedDate
	private Date updatedDate;
	@LastModifiedBy
	private Long updatedBy;
}
