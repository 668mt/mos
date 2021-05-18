package mt.spring.mos.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @Author Martin
 * @Date 2020/9/13
 */
@Controller
public class IndexController {
	@GetMapping("/")
	public String index() {
		return "redirect:/index.html";
	}
}
