package mt.spring.mos.server.entity.dto;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2020/9/13
 */
@Data
public class AccessControlUpdateDto {
	private String useInfo;
	private Long bucketId;
	private Long openId;
}
