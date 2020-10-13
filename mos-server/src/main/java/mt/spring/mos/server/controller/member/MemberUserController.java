package mt.spring.mos.server.controller.member;

import mt.common.annotation.CurrentUser;
import mt.common.entity.ResResult;
import mt.spring.mos.server.entity.dto.MemberUserUpdateDTO;
import mt.spring.mos.server.entity.dto.UserUpdateDTO;
import mt.spring.mos.server.entity.po.User;
import mt.spring.mos.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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
	
	@PutMapping
	public ResResult update(@RequestBody MemberUserUpdateDTO memberUserUpdateDTO, @CurrentUser User currentUser) {
		UserUpdateDTO userUpdateDTO = new UserUpdateDTO();
		userUpdateDTO.setId(currentUser.getId());
		userUpdateDTO.setName(memberUserUpdateDTO.getName());
		userUpdateDTO.setPassword(memberUserUpdateDTO.getPassword());
		userService.updateUser(userUpdateDTO);
		return ResResult.success();
	}
}
