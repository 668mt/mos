package mt.spring.mos.server.controller.member;

import mt.common.annotation.CurrentUser;
import mt.common.entity.ResResult;
import mt.spring.mos.sdk.utils.Assert;
import mt.spring.mos.server.entity.dto.MemberUserUpdateDTO;
import mt.spring.mos.server.entity.dto.UserUpdateDTO;
import mt.spring.mos.server.entity.po.User;
import mt.spring.mos.server.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author Martin
 * @Date 2020/10/13
 */
@RestController
@RequestMapping("/member/user")
public class MemberUserController {
	@Autowired
	private UserService userService;
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@PutMapping
	public ResResult update(@RequestBody MemberUserUpdateDTO memberUserUpdateDTO, @CurrentUser User currentUser) {
		String password = currentUser.getPassword();
		UserUpdateDTO userUpdateDTO = new UserUpdateDTO();
		userUpdateDTO.setId(currentUser.getId());
		userUpdateDTO.setName(memberUserUpdateDTO.getName());
		if (StringUtils.isNotBlank(memberUserUpdateDTO.getPassword())) {
			Assert.notNull(memberUserUpdateDTO.getCurrentPassword(), "当前密码不能为空");
			Assert.state(passwordEncoder.matches(memberUserUpdateDTO.getCurrentPassword(), password), "当前密码不正确");
			currentUser.setPassword(memberUserUpdateDTO.getPassword());
		}
		userUpdateDTO.setPassword(memberUserUpdateDTO.getPassword());
		userService.updateUser(userUpdateDTO);
		return ResResult.success();
	}
	
}
