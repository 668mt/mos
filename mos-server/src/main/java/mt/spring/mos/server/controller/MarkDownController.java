package mt.spring.mos.server.controller;

import mt.spring.mos.server.utils.UrlEncodeUtils;
import mt.utils.http.MyHttp;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @Author Martin
 * @Date 2020/10/13
 */
@Controller
@RequestMapping("/markdown")
public class MarkDownController {
	
	@GetMapping("/show")
	public String show(ModelMap modelMap, String base64Url, String title) {
		MyHttp myHttp = new MyHttp(UrlEncodeUtils.base64Decode(base64Url));
		myHttp.setEncode("UTF-8");
		String content = myHttp.connect();
		modelMap.addAttribute("content", content);
		modelMap.addAttribute("title", title);
		return "markdown/index";
	}
	
	@GetMapping("/online")
	public String online() {
		return "markdown/online";
	}
}
