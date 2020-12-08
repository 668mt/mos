package mt.spring.mos.server.controller.member;

import mt.common.annotation.CurrentUser;
import mt.common.entity.ResResult;
import mt.spring.mos.server.entity.dto.DirUpdateDto;
import mt.spring.mos.server.entity.po.User;
import mt.spring.mos.server.service.DirService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @Author Martin
 * @Date 2020/12/5
 */
@RestController
@RequestMapping("/member/dir")
public class DirController {
	
	@Autowired
	private DirService dirService;
	
	@PutMapping("/{bucketName}/{id}")
	public ResResult update(@ApiIgnore @CurrentUser User currentUser, @PathVariable String bucketName, @PathVariable Long id, @RequestBody DirUpdateDto dirUpdateDto) {
		dirUpdateDto.setBucketName(bucketName);
		dirUpdateDto.setId(id);
		dirService.updatePath(currentUser.getId(), dirUpdateDto);
		return ResResult.success();
	}
}
