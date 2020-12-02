package mt.spring.mos.client.entity.dto;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2020/11/21
 */
@Data
public class MergeFileDto {
	private String path;
	private Integer chunks;
	private String desPathname;
	private boolean getMd5;
	private boolean encode;
}
