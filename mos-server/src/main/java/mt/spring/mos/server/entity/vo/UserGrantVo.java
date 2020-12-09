package mt.spring.mos.server.entity.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.spring.mos.server.entity.BucketPerm;
import mt.spring.mos.server.entity.po.User;

import javax.persistence.Transient;
import java.util.List;

/**
 * @Author Martin
 * @Date 2020/10/8
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class UserGrantVo extends User {
	@Transient
	private String key;
	@Transient
	private String title;
	@Transient
	private List<BucketPerm> perms;
}
