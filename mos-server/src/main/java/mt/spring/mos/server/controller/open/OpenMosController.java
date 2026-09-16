package mt.spring.mos.server.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mt.common.entity.ResResult;
import mt.spring.mos.server.annotation.OpenApi;
import mt.spring.mos.server.entity.BucketPerm;
import mt.spring.mos.server.entity.dto.ShortUrlCreateRequest;
import mt.spring.mos.server.entity.dto.ShortUrlResponse;
import mt.spring.mos.server.service.OpenMosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

/**
 * @Author Martin
 * @Date 2021/1/16
 */
@RestController
public class OpenMosController {
	@Autowired
	private OpenMosService openMosService;
	
	@GetMapping("/mos/{bucketName}/**")
	@Operation(summary = "获取资源")
	@OpenApi(pathnamePrefix = "/mos/{bucketName}", perms = BucketPerm.SELECT)
	public ModelAndView mos(@RequestParam(defaultValue = "false") Boolean thumb,
							@PathVariable String bucketName,
							@RequestParam(defaultValue = "false") Boolean render,
							@RequestParam(defaultValue = "false") Boolean gallary,
							HttpServletRequest request,
							HttpServletResponse httpServletResponse
	) throws Exception {
		httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
		httpServletResponse.setHeader("Access-Control-Allow-Credentials", "true");
		httpServletResponse.setHeader("Access-Control-Expose-Headers", "*");
		httpServletResponse.setHeader("Access-Control-Allow-Headers", "*");
		String pathname = openMosService.getPathname(request, "/mos/" + bucketName);
		return openMosService.requestResource(bucketName, pathname, thumb, render, gallary, request, httpServletResponse);
	}
	
	@PostMapping("/s")
	@Operation(summary = "创建短链接")
	@OpenApi(perms = BucketPerm.SELECT)
	public ResResult<ShortUrlResponse> createShortUrl(@RequestParam String bucketName,
													 @RequestParam String pathname,
													 @RequestParam String sign,
													 @RequestBody ShortUrlCreateRequest request) {
		String shortCode = openMosService.createShortUrl(bucketName, pathname, sign, request.getQueryString(), request.getExpireSeconds());
		ShortUrlResponse response = new ShortUrlResponse();
		response.setShortCode(shortCode);
		return ResResult.success(response);
	}
	
	@GetMapping("/s/{shortCode}")
	@Operation(summary = "短链接解析跳转")
	public void resolveShortUrl(@PathVariable String shortCode,
								 HttpServletResponse response) throws Exception {
		OpenMosService.ShortUrlTarget target = openMosService.resolveShortUrl(shortCode);
		if (target == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		StringBuilder location = new StringBuilder("/mos/")
			.append(target.getBucketName())
			.append(target.getPathname())
			.append("?sign=").append(target.getSign());
		if (target.getQueryString() != null) {
			location.append('&').append(target.getQueryString());
		}
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Credentials", "true");
		response.setHeader("Access-Control-Expose-Headers", "*");
		response.setHeader("Access-Control-Allow-Headers", "*");
		response.sendRedirect(location.toString());
	}
	
	
}
