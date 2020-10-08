package mt.spring.mos.server.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/10/8
 */
@Data
public class BucketGrantDto {
	private Long bucketId;
	private List<Long> userIds;
}
