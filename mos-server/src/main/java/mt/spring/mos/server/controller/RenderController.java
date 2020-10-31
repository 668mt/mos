package mt.spring.mos.server.controller;

import mt.spring.mos.server.utils.UrlEncodeUtils;
import mt.utils.http.MyHttp;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.DefaultUriBuilderFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2020/10/13
 */
@Controller
@RequestMapping
public class RenderController {
	private static final DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory();
	
	@GetMapping("/render/show")
	public String showMarkDown(HttpServletRequest request, ModelMap modelMap, String base64Url, String title, String templatePath, String charset) {
		MyHttp myHttp = new MyHttp(uriFactory.expand(UrlEncodeUtils.base64Decode(base64Url)).toString());
		if (StringUtils.isBlank(charset)) {
			charset = "UTF-8";
		}
		myHttp.setEncode(charset);
		String content = myHttp.connect();
		Map<String, String[]> parameterMap = request.getParameterMap();
		if (parameterMap != null) {
			for (Map.Entry<String, String[]> stringEntry : parameterMap.entrySet()) {
				modelMap.addAttribute(stringEntry.getKey(), StringUtils.join(stringEntry.getValue(), ","));
			}
		}
		modelMap.addAttribute("content", content);
		modelMap.addAttribute("title", title);
		return templatePath;
	}
	
	@GetMapping("/markdown/online")
	public String online() {
		return "markdown/online";
	}
	
}
