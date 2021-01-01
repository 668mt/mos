package mt.spring.mos.server.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/12/26
 */
@Data
public class ResourceCopyDto {
	private List<Long> resourceIds;
	private List<Long> dirIds;
}
