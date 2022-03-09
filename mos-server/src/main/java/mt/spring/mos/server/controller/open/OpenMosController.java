package mt.spring.mos.server.controller.open;

import io.swagger.annotations.ApiOperation;
import mt.spring.mos.server.annotation.OpenApi;
import mt.spring.mos.server.entity.BucketPerm;
import mt.spring.mos.server.service.OpenMosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author Martin
 * @Date 2021/1/16
 */
@RestController
public class OpenMosController {
	@Autowired
	private OpenMosService openMosService;
	
	@GetMapping("/mos/{bucketName}/**")
	@ApiOperation("获取资源")
	@OpenApi(pathnamePrefix = "/mos/{bucketName}", perms = BucketPerm.SELECT)
	@CrossOrigin(origins = "*", allowCredentials = "true")
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
		return openMosService.requestResouce(bucketName, pathname, thumb, render, gallary, request, httpServletResponse);
	}
}
