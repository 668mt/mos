package mt.spring.mos.server.entity.dto;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2021/1/11
 */
@Data
public class ResourceSearchDto {
	private String sortField;
	private String sortOrder;
	private String keyWord;
	private Integer pageNum;
	private Integer pageSize;
	private String path;
}
