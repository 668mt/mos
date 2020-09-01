package mt.common.entity;


import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import mt.common.annotation.CreatedDate;
import mt.common.annotation.GenerateOrder;
import mt.common.annotation.LastModifiedDate;
import mt.common.annotation.Version;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Martin
 * @ClassName: BaseFields
 * @Description:
 * @date 2018-3-30 下午5:57:57
 */
@Data
public class BaseFields implements Serializable {
	
	private static final long serialVersionUID = 4861455153422740537L;
	
	@CreatedDate
	@GenerateOrder(5)
	@ApiModelProperty(hidden = true)
	private Date createdDate;
	
	@Version
	@GenerateOrder(5)
	@ApiModelProperty(hidden = true)
	private Long version;
	
	@LastModifiedDate
	@GenerateOrder(5)
	@ApiModelProperty(hidden = true)
	private Date lastModifiedDate;
}
