package mt.spring.mos.server.entity;

import lombok.Data;
import mt.common.annotation.CreatedByUserName;
import mt.common.annotation.CreatedDate;
import mt.common.annotation.UpdatedByUserName;
import mt.common.annotation.UpdatedDate;

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
	@CreatedByUserName
	private String createdBy;
	@UpdatedDate
	private Date updatedDate;
	@UpdatedByUserName
	private String updatedBy;
}
