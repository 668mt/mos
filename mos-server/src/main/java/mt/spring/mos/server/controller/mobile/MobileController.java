package mt.spring.mos.server.controller.mobile;

import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * @Author Martin
 * @Date 2021/12/18
 */
@RestController
@RequestMapping("/mobile")
public class MobileController {
	@GetMapping
	public void index(HttpServletResponse response) throws IOException {
		response.setContentType("text/html;charset=utf-8");
		ServletOutputStream outputStream = response.getOutputStream();
		try (InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("resources/mobile/index.html")) {
			IOUtils.copy(resourceAsStream, outputStream);
		}
	}
}
