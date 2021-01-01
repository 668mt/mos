package mt.spring.mos.server.controller.member;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import mt.common.annotation.CurrentUser;
import mt.common.entity.ResResult;
import mt.common.mybatis.utils.MapperColumnUtils;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.annotation.NeedPerm;
import mt.spring.mos.server.entity.BucketPerm;
import mt.spring.mos.server.entity.dto.ResourceCopyDto;
import mt.spring.mos.server.entity.dto.ResourceUpdateDto;
import mt.spring.mos.server.entity.po.Audit;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Dir;
import mt.spring.mos.server.entity.po.User;
import mt.spring.mos.server.entity.vo.DirAndResourceVo;
import mt.spring.mos.server.service.*;
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
import java.util.Arrays;
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
	@Autowired
	private AuditService auditService;
	@Autowired
	private BucketGrantService bucketGrantService;
	public final List<String> sortFields = Arrays.asList("path", "sizeByte", "createdDate", "createdBy", "updatedDate", "updatedBy", "isPublic", "contentType", "visits");
	
	@DeleteMapping("/{bucketName}/del")
	@NeedPerm(BucketPerm.DELETE)
	public ResResult del(@PathVariable String bucketName, Long[] dirIds, Long[] fileIds, @CurrentUser User currentUser) {
		Assert.state(dirIds != null || fileIds != null, "要删除的文件或文件夹不能为空");
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "bucket不能为空");
		resourceService.deleteResources(bucket, dirIds, fileIds);
		return ResResult.success();
	}
	
	@PutMapping("/{bucketName}/{id}")
	@NeedPerm(BucketPerm.UPDATE)
	public ResResult update(@PathVariable String bucketName, @PathVariable Long id, @RequestBody ResourceUpdateDto resourceUpdateDto, @CurrentUser User currentUser) {
		resourceUpdateDto.setId(id);
		resourceService.updateResource(resourceUpdateDto, currentUser.getId(), bucketName);
		return ResResult.success();
	}
	
	@GetMapping("/{bucketName}/**")
	@NeedPerm(BucketPerm.SELECT)
	public ResResult list(String sortField, String sortOrder, String keyWord, @PathVariable String bucketName, Integer pageNum, Integer pageSize, HttpServletRequest request, @ApiIgnore @CurrentUser User currentUser) throws UnsupportedEncodingException {
		String requestURI = request.getRequestURI();
		String path = requestURI.substring(("/member/resource/" + bucketName).length());
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		if (StringUtils.isBlank(path)) {
			path = "/";
		}
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "bucket不存在");
		auditService.doAudit(bucket.getId(), path, Audit.Type.READ, Audit.Action.list, null, 0);
		List<Filter> filters2 = new ArrayList<>();
		filters2.add(new Filter("path", eq, URLDecoder.decode(path, "UTF-8")));
		filters2.add(new Filter("bucketId", eq, bucket.getId()));
		Dir dir = dirService.findOneByFilters(filters2);
		List<Dir> parentDirs = Collections.emptyList();
		JSONObject data = new JSONObject();
		if (dir != null) {
			parentDirs = dirService.findAllParentDir(dir);
			Collections.reverse(parentDirs);
			if ("readableSize".equals(sortField)) {
				sortField = "sizeByte";
			}
			if ("name".equals(sortField)) {
				sortField = "path";
			}
			if (StringUtils.isNotBlank(sortOrder) && StringUtils.isNotBlank(sortField) && sortFields.contains(sortField)) {
				String order = "descend".equalsIgnoreCase(sortOrder) ? "desc" : "asc";
				sortField = MapperColumnUtils.parseColumn(sortField);
				PageHelper.orderBy("is_dir desc ," + sortField + " " + order);
			}
			PageInfo<DirAndResourceVo> resources = resourceService.findDirAndResourceVoListPage(keyWord, pageNum, pageSize, bucket.getId(), dir.getId());
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
	
	@PutMapping("/copy/{bucketName}/to/{desBucketName}")
	@NeedPerm(BucketPerm.SELECT)
	public ResResult copy(@ApiIgnore @CurrentUser User currentUser, @PathVariable String bucketName, @PathVariable String desBucketName, @RequestBody ResourceCopyDto resourceCopyDto) {
		Bucket srcBucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Bucket desBucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), desBucketName);
		Assert.state(bucketGrantService.hasPerms(currentUser.getId(), desBucket, BucketPerm.INSERT), desBucketName + "没有权限");
//		resourceService.copyToBucket(resourceCopyDto, srcBucket, desBucket);
		return ResResult.success();
	}
}
