package mt.spring.mos.server.controller.member;

import com.github.pagehelper.PageHelper;
import io.swagger.annotations.ApiOperation;
import mt.common.annotation.CurrentUser;
import mt.common.entity.ResResult;
import mt.common.tkmapper.Filter;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.server.annotation.NeedPerm;
import mt.spring.mos.server.entity.BucketPerm;
import mt.spring.mos.server.entity.dto.DirAddDto;
import mt.spring.mos.server.entity.dto.DirUpdateDto;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Dir;
import mt.spring.mos.server.entity.po.User;
import mt.spring.mos.server.service.BucketService;
import mt.spring.mos.server.service.DirService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Martin
 * @Date 2020/12/5
 */
@RestController
@RequestMapping("/member/dir")
public class DirController {
	
	@Autowired
	private DirService dirService;
	@Autowired
	private BucketService bucketService;
	
	@GetMapping("/{bucketName}/select")
	@NeedPerm(BucketPerm.SELECT)
	@ApiOperation("模糊查找")
	public ResResult selectByPath(@RequestParam(required = false, defaultValue = "30") Integer pageSize, @PathVariable String bucketName, @ApiIgnore @CurrentUser User currentUser, String path) {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "不存在bucket：" + bucketName);
		if (path == null) {
			path = "/";
		}
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		PageHelper.startPage(1, pageSize, "(length(path) - length(replace(path,'/',''))) asc");
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("bucketId", Filter.Operator.eq, bucket.getId()));
		filters.add(new Filter("path", Filter.Operator.like, '%' + path + '%'));
		return ResResult.success(dirService.findByFilters(filters));
	}
	
	@GetMapping("/{bucketName}/findByPath")
	@NeedPerm(BucketPerm.SELECT)
	@ApiOperation("精确查找")
	public ResResult findByPath(@ApiIgnore @CurrentUser User currentUser, @PathVariable String bucketName, String path) {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "不存在bucket：" + bucketName);
		Dir dir = dirService.findOneByPathAndBucketId(path, bucket.getId());
		return ResResult.success(dir);
	}
	
	@PutMapping("/{bucketName}/{id}")
	@NeedPerm(BucketPerm.UPDATE)
	@ApiOperation("修改")
	public ResResult update(@ApiIgnore @CurrentUser User currentUser, @PathVariable String bucketName, @PathVariable Long id, @RequestBody DirUpdateDto dirUpdateDto) {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "不存在bucket：" + bucketName);
		dirUpdateDto.setBucketName(bucketName);
		dirUpdateDto.setId(id);
		dirService.updatePath(bucket.getId(), dirUpdateDto);
		return ResResult.success();
	}
	
	@PutMapping("/{bucketName}/merge/{srcId}/to/{desId}")
	@NeedPerm(BucketPerm.UPDATE)
	@ApiOperation("合并")
	public ResResult merge(@ApiIgnore @CurrentUser User currentUser, @PathVariable String bucketName, @PathVariable Long srcId, @PathVariable Long desId) {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "不存在bucket：" + bucketName);
		dirService.mergeDir(bucket.getId(), srcId, desId);
		return ResResult.success();
	}
	
	
	@PostMapping("/{bucketName}")
	@NeedPerm(BucketPerm.INSERT)
	@ApiOperation("新增")
	public ResResult add(@ApiIgnore @CurrentUser User currentUser, @PathVariable String bucketName, @RequestBody DirAddDto dirAddDto) {
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(currentUser.getId(), bucketName);
		Assert.notNull(bucket, "不存在bucket：" + bucketName);
		dirService.addDir(dirAddDto.getPath(), bucket.getId());
		return ResResult.success();
	}
}
