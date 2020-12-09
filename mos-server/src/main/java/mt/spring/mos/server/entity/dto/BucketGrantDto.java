package mt.spring.mos.server.entity.dto;

import lombok.Data;
import mt.spring.mos.server.entity.BucketPerm;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/10/8
 */
@Data
public class BucketGrantDto {
	private Long bucketId;
	private List<GrantBody> grants;
	
	@Data
	public static class GrantBody {
		private Long userId;
		private List<BucketPerm> perms;
	}
}
