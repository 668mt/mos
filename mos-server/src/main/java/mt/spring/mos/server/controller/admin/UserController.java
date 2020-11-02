package mt.spring.mos.server.controller.admin;

import io.swagger.annotations.Api;
import mt.common.entity.ResResult;
import mt.spring.mos.server.entity.dto.UserAddDTO;
import mt.spring.mos.server.entity.dto.UserUpdateDTO;
import mt.spring.mos.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Author Martin
 * @Date 2020/5/25
 */
@RestController
@RequestMapping("/admin/user")
@Api(tags = "用户管理")
public class UserController {
	
	@Autowired
	private UserService userService;
	
	@PostMapping
	public ResResult addUser(@RequestBody UserAddDTO user) {
		return ResResult.success(userService.addUser(user));
	}
	
	@PutMapping
	public ResResult updateUser(@RequestBody UserUpdateDTO userUpdateDTO) {
		return ResResult.success(userService.updateUser(userUpdateDTO));
	}
	
	@PutMapping("/unlock/{username}")
	public ResResult unlock(@PathVariable String username) {
		userService.unlock(username);
		return ResResult.success();
	}
	
	@DeleteMapping("/{id}")
	public ResResult delUser(@PathVariable Long id) {
		return ResResult.success(userService.deleteUser(id));
	}
	
	@GetMapping("/page/list")
	public ResResult pageList(Integer pageNum, Integer pageSize) {
		return ResResult.success(userService.findPage(pageNum, pageSize, null, null));
	}
}
