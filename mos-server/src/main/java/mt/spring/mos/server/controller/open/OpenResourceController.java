package mt.spring.mos.server.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import mt.common.entity.ResResult;
import mt.spring.mos.server.annotation.OpenApi;
import mt.spring.mos.server.entity.BucketPerm;
import mt.spring.mos.server.entity.dto.ResourceSearchDto;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.FileHouse;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.service.AuditService;
import mt.spring.mos.server.service.FileHouseService;
import mt.spring.mos.server.service.ResourceService;
import mt.utils.common.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.github.xiaoymin.knife4j.annotations.Ignore;

/**
 * @Author Martin
 * @Date 2021/1/16
 */
@RestController
@RequestMapping("/open/resource")
public class OpenResourceController {
	@Autowired
	private AuditService auditService;
	@Autowired
	private ResourceService resourceService;
	@Autowired
	private FileHouseService fileHouseService;
	
	@GetMapping("/{bucketName}/list")
	@Operation(summary = "查询文件列表")
	@OpenApi(perms = BucketPerm.SELECT)
	public ResResult list(@PathVariable String bucketName,
						  @RequestParam(name = "path") String pathname,
						  ResourceSearchDto resourceSearchDto,
						  @Ignore Bucket bucket
	) {
		resourceSearchDto.setPath(pathname);
		auditService.readRequestsRecord(bucket.getId(), 1);
		return ResResult.success(resourceService.findDirAndResourceVoListPage(resourceSearchDto, bucket.getId()));
	}
	
	@GetMapping("/{bucketName}/info")
	@Operation(summary = "查询文件信息")
	@OpenApi(perms = BucketPerm.SELECT)
	public ResResult info(@PathVariable String bucketName,
						  String pathname,
						  @Ignore Bucket bucket
	) {
		auditService.readRequestsRecord(bucket.getId(), 1);
		Resource resource = resourceService.findResourceByPathnameAndBucketId(pathname, bucket.getId(), false);
		Assert.notNull(resource, "资源" + pathname + "不存在");
		Long fileHouseId = resource.getFileHouseId();
		if (fileHouseId != null) {
			FileHouse fileHouse = fileHouseService.findById(fileHouseId);
			resource.setMd5(fileHouse.getMd5());
		}
		return ResResult.success(resource);
	}
	
	@OpenApi(perms = BucketPerm.DELETE)
	@Operation(summary = "删除文件")
	@DeleteMapping("/{bucketName}/deleteFile")
	public ResResult deleteFile(String pathname, @PathVariable String bucketName, Bucket bucket) {
		auditService.writeRequestsRecord(bucket.getId(), 1);
		resourceService.realDeleteResource(bucket.getId(), pathname);
		return ResResult.success();
	}
	
	@GetMapping("/{bucketName}/isExists")
	@Operation(summary = "判断文件是否存在")
	@OpenApi(perms = BucketPerm.SELECT)
	public ResResult isExists(String pathname, @PathVariable String bucketName, Bucket bucket) {
		auditService.readRequestsRecord(bucket.getId(), 1);
		Resource resource = resourceService.findResourceByPathnameAndBucketId(pathname, bucket.getId(), false);
		return ResResult.success(resource != null);
	}
	
	@PutMapping("/{bucketName}/rename")
	@Operation(summary = "修改文件名")
	@OpenApi(perms = BucketPerm.UPDATE)
	public ResResult rename(Bucket bucket, @PathVariable String bucketName, String pathname, String desPathname) {
		Assert.notNull(pathname, "文件路径不能为空");
		Assert.notNull(desPathname, "目标文件路径不能为空");
		auditService.writeRequestsRecord(bucket.getId(), 1);
		resourceService.rename(bucketName, pathname, desPathname);
		return ResResult.success();
	}
}
