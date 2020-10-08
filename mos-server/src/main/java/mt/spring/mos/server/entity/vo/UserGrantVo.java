package mt.spring.mos.server.entity.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.spring.mos.server.entity.po.User;

import javax.persistence.Transient;

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
}
