package mt.spring.mos.server.controller;

import mt.spring.mos.server.service.BucketService;
import mt.spring.mos.server.service.DirService;
import mt.spring.mos.server.service.ResourceService;
import com.github.pagehelper.PageHelper;
import mt.common.annotation.CurrentUser;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Dir;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.entity.po.User;
import mt.utils.MyUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static mt.common.tkmapper.Filter.Operator.eq;

/**
 * @Author Martin
 * @Date 2020/5/20
 */
@Controller
@RequestMapping("/list")
public class IndexController {
	@Autowired
	private ResourceService resourceService;
	@Autowired
	private DirService dirService;
	@Autowired
	private BucketService bucketService;
	
	@GetMapping
	public String bucketList(@CurrentUser User currentUser, ModelMap modelMap) {
		modelMap.addAttribute("buckets", bucketService.findList("userId", currentUser.getId()));
		return "bucket";
	}
	
	@GetMapping("/{bucketName}/**")
	public String list(@PathVariable String bucketName, HttpServletRequest request, ModelMap modelMap, @CurrentUser User currentUser) throws UnsupportedEncodingException {
		String requestURI = request.getRequestURI();
		String path = requestURI.substring(("/list/" + bucketName).length());
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
		modelMap.addAttribute("files", files);
		modelMap.addAttribute("dirs", dirs);
		modelMap.addAttribute("parentDirs", parentDirs);
		modelMap.addAttribute("currentDir", dir);
		modelMap.addAttribute("bucketName", bucketName);
		modelMap.addAttribute("buckets", bucketService.findList("userid", currentUser.getId()));
		if (MyUtils.isNotEmpty(parentDirs)) {
			modelMap.addAttribute("lastDir", parentDirs.get(parentDirs.size() - 1));
		}
		return "index";
	}
}
