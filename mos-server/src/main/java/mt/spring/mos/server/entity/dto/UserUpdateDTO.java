package mt.spring.mos.server.entity.dto;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2020/5/25
 */
@Data
public class UserUpdateDTO {
	private Long id;
	private String name;
	private String username;
	private String password;
	private Boolean isEnable;
	private Boolean isAdmin;
}
