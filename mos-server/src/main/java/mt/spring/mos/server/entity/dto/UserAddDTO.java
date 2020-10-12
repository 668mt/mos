package mt.spring.mos.server.entity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @Author Martin
 * @Date 2020/5/25
 */
@Data
public class UserAddDTO {
	@NotBlank(message = "姓名不能为空")
	private String name;
	@NotBlank(message = "用户名不能为空")
	private String username;
	@NotBlank(message = "密码不能为空")
	private String password;
	private Boolean isEnable;
	private Boolean isAdmin = false;
	
}
