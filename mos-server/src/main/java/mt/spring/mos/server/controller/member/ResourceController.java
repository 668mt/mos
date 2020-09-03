package mt.spring.mos.server.controller.member;

import mt.spring.mos.server.service.BucketService;
import mt.spring.mos.server.service.DirService;
import mt.spring.mos.server.service.ResourceService;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import mt.common.annotation.CurrentUser;
import mt.common.entity.ResResult;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Dir;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.entity.po.User;
import mt.utils.Assert;
import mt.utils.MyUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
	
	@DeleteMapping("/{bucketName}/delFile")
	public ResResult delFile(@PathVariable String bucketName, String pathname, @CurrentUser User currentUser) {
		Assert.notNull(pathname);
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "bucket不能为空");
		resourceService.deleteResource(bucket, pathname);
		return ResResult.success();
	}
	
	@DeleteMapping("/{bucketName}/delDir")
	public ResResult delDir(@PathVariable String bucketName, String path, @CurrentUser User currentUser) {
		Assert.notNull(path);
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "bucket不能为空");
		resourceService.deleteDir(bucket, path);
		return ResResult.success();
	}
	
	
	@Autowired
	private DirService dirService;
	
	@GetMapping("/{bucketName}/**")
	public ResResult list(@PathVariable String bucketName, HttpServletRequest request, @CurrentUser User currentUser) throws UnsupportedEncodingException {
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
		List<Dir> dirs = Collections.emptyList();
		List<Resource> files = Collections.emptyList();
		if (dir != null) {
			Assert.notNull(dir, "文件路径不存在");
			parentDirs = dirService.findAllParentDir(dir);
			Collections.reverse(parentDirs);
			PageHelper.orderBy("path");
			dirs = dirService.findList("parentId", dir.getId());
			PageHelper.orderBy("pathname");
			files = resourceService.findList("dirId", dir.getId());
		}
		JSONObject data = new JSONObject();
		data.put("files", files);
		data.put("dirs", dirs);
		data.put("parentDirs", parentDirs);
		data.put("currentDir", dir);
		data.put("bucketName", bucketName);
		data.put("buckets", bucketService.findList("userId", currentUser.getId()));
		if (MyUtils.isNotEmpty(parentDirs)) {
			data.put("lastDir", parentDirs.get(parentDirs.size() - 1));
		}
		return ResResult.success(data);
	}
}
