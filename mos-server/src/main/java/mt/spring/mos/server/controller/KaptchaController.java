package mt.spring.mos.server.controller;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mt.common.entity.ResResult;
import mt.spring.mos.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

@RestController
public class KaptchaController {
	/**
	 * 验证码工具
	 */
	@Autowired
	private DefaultKaptcha defaultKaptcha;
	@Autowired
	private UserService userService;
	
	@GetMapping("/kaptcha/check/{username}")
	public ResResult isNeed(@PathVariable String username) {
		return ResResult.success(userService.isNeedCode(username));
	}
	
	@GetMapping("/kaptcha")
	public void defaultKaptcha(HttpServletRequest request, HttpServletResponse response) throws Exception {
		byte[] captcha;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		try {
			// 将生成的验证码保存在session中
			String createText = defaultKaptcha.createText();
			request.getSession().setAttribute("kaptchaCode", createText);
			BufferedImage bi = defaultKaptcha.createImage(createText);
			ImageIO.write(bi, "jpg", out);
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		captcha = out.toByteArray();
		response.setHeader("Cache-Control", "no-store");
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", 0);
		response.setContentType("image/jpeg");
		jakarta.servlet.ServletOutputStream sout = response.getOutputStream();
		sout.write(captcha);
		sout.flush();
		sout.close();
	}
}