package mt.spring.mos.server.entity.dto;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2020/5/25
 */
@Data
public class MemberUserUpdateDTO {
	private String name;
	private String currentPassword;
	private String password;
}
