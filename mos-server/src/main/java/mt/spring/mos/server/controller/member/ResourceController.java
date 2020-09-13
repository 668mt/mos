package mt.spring.mos.server.controller.member;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import mt.common.annotation.CurrentUser;
import mt.common.entity.ResResult;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Dir;
import mt.spring.mos.server.entity.po.User;
import mt.spring.mos.server.entity.vo.DirAndResourceVo;
import mt.spring.mos.server.service.BucketService;
import mt.spring.mos.server.service.DirService;
import mt.spring.mos.server.service.ResourceService;
import mt.utils.MyUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static mt.common.tkmapper.Filter.Operator.eq;

/**
 * @Author Martin
 * @Date 2020/5/29
 */
@RestController
@RequestMapping("/member/resource")
public class ResourceController {
	@Autowired
	private ResourceService resourceService;
	@Autowired
	private BucketService bucketService;
	@Autowired
	private DirService dirService;
	
	@DeleteMapping("/{bucketName}/del")
	public ResResult del(@PathVariable String bucketName, Long[] dirIds, Long[] fileIds, @CurrentUser User currentUser) {
		Assert.state(dirIds != null || fileIds != null, "要删除的文件或文件夹不能为空");
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "bucket不能为空");
		resourceService.deleteResources(bucket, dirIds, fileIds);
		return ResResult.success();
	}
	
	@GetMapping("/{bucketName}/**")
	public ResResult list(String keyWord, @PathVariable String bucketName, Integer pageNum, Integer pageSize, HttpServletRequest request, @ApiIgnore @CurrentUser User currentUser) throws UnsupportedEncodingException {
		String requestURI = request.getRequestURI();
		String path = requestURI.substring(("/member/resource/" + bucketName).length());
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		if (StringUtils.isBlank(path)) {
			path = "/";
		}
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("bucketName", eq, bucketName));
		filters.add(new Filter("userId", eq, currentUser.getId()));
		Bucket bucket = bucketService.findOneByFilters(filters);
		Assert.notNull(bucket, "bucket不存在");
		
		List<Filter> filters2 = new ArrayList<>();
		filters2.add(new Filter("path", eq, URLDecoder.decode(path, "UTF-8")));
		filters2.add(new Filter("bucketId", eq, bucket.getId()));
		Dir dir = dirService.findOneByFilters(filters2);
		List<Dir> parentDirs = Collections.emptyList();
		JSONObject data = new JSONObject();
		if (dir != null) {
			parentDirs = dirService.findAllParentDir(dir);
			Collections.reverse(parentDirs);
			PageInfo<DirAndResourceVo> resources = resourceService.findDirAndResourceVoListPage(keyWord, pageNum, pageSize, currentUser.getId(), dir.getId());
			data.put("resources", resources);
		}
		data.put("parentDirs", parentDirs);
		data.put("currentDir", dir);
		data.put("bucketName", bucketName);
		if (MyUtils.isNotEmpty(parentDirs)) {
			data.put("lastDir", parentDirs.get(parentDirs.size() - 1));
		}
		return ResResult.success(data);
	}
}
