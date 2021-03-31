package mt.spring.mos.server.entity.dto;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2021/3/28
 */
@Data
public class RecoverDto {
	private Long[] fileIds;
	private Long[] dirIds;
}
