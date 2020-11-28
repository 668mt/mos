package mt.spring.mos.server.controller.admin;

import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import mt.common.entity.ResResult;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Author Martin
 * @Date 2020/11/28
 */
@RestController
@RequestMapping("/admin/client")
@Api(tags = "资源客户端管理")
public class ClientController {
	@Autowired
	private ClientService clientService;
	
	@GetMapping("/list")
	public ResResult list(Integer pageNum, Integer pageSize) {
		PageInfo<Client> page = clientService.findPage(pageNum, pageSize, null, null);
		return ResResult.success(page);
	}
	
	@DeleteMapping("/kick/{clientId}")
	public ResResult kick(@PathVariable String clientId) {
		clientService.kick(clientId);
		return ResResult.success();
	}
	
	@PutMapping("/recover/{clientId}")
	public ResResult recover(@PathVariable String clientId) {
		clientService.recover(clientId);
		return ResResult.success();
	}
}
