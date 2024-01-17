package mt.spring.mos.server.entity.dto;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2020/9/13
 */
@Data
public class SignDto {
	private Long openId;
	private String bucketName;
	private Long resourceId;
	private Long dirId;
	private Long expireSeconds;
	private Boolean render;
	
	public Boolean getRender() {
		return render != null && render;
	}
}
