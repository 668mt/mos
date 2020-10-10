package mt.spring.mos.server.entity.dto;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2020/10/10
 */
@Data
public class ResourceUpdateDto {
	private Long id;
	private String contentType;
	private Boolean isPublic;
}
