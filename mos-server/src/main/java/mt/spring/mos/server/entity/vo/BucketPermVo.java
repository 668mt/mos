package mt.spring.mos.server.entity.vo;

import lombok.Data;
import mt.spring.mos.server.entity.BucketPerm;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/12/9
 */
@Data
public class BucketPermVo {
	private String bucketName;
	private List<BucketPerm> perms;
}
