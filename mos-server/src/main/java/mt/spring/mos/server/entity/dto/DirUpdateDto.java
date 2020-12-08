package mt.spring.mos.server.entity.dto;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2020/12/5
 */
@Data
public class DirUpdateDto {
	private Long id;
	private String bucketName;
	private String path;
}
