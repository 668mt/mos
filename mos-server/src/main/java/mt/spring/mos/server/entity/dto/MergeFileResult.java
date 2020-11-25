package mt.spring.mos.server.entity.dto;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2020/11/25
 */
@Data
public class MergeFileResult {
	private Long length;
	private String md5;
}
