package mt.spring.mos.server.controller.open;

import com.github.pagehelper.PageHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import mt.common.entity.ResResult;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.annotation.OpenApi;
import mt.spring.mos.server.entity.BucketPerm;
import mt.spring.mos.server.entity.dto.InitUploadDto;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.FileHouse;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.service.*;
import mt.utils.common.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @Author Martin
 * @Date 2020/5/15
 */
@RestController
@RequestMapping("/")
@Api(tags = "开放接口")
@Slf4j
public class OpenUploadController {
	@Autowired
	private ResourceService resourceService;
	@Autowired
	private ClientService clientService;
	@Autowired
	private BucketService bucketService;
	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;
	@Autowired
	private FileHouseService fileHouseService;
	@Autowired
	private FileHouseItemService fileHouseItemService;
	@Autowired
	private FileHouseRelaClientService fileHouseRelaClientService;
	@Autowired
	private AuditService auditService;
	@Autowired
	private UploadFileService uploadFileService;
	
	
	@PostMapping("/upload/{bucketName}/init")
	@ApiOperation("上传初始化")
	@OpenApi(perms = BucketPerm.INSERT)
	public ResResult initUpload(@RequestParam(defaultValue = "false") Boolean isPublic,
								String contentType,
								@PathVariable String bucketName,
								String pathname,
								String totalMd5,
								Long totalSize,
								Long lastModified,
								Integer chunks,
								@RequestParam(defaultValue = "false") Boolean cover
	) {
		Assert.notNull(pathname, "pathname不能为空");
		Assert.notNull(totalMd5, "totalMd5不能为空");
		Assert.notNull(totalSize, "totalSize不能为空");
		Assert.notNull(chunks, "chunks不能为空");
		Assert.state(chunks > 0, "chunks必须大于0");
		Bucket bucket = bucketService.findOne("bucketName", bucketName);
		Assert.notNull(bucket, "bucket不存在:" + bucketName);
		auditService.writeRequestsRecord(bucket.getId(), 1);
		Resource findResource = resourceService.findResourceByPathnameAndBucketId(pathname, bucket.getId(), false);
		if (!cover) {
			Assert.isNull(findResource, "已存在相同的pathname");
		}
		
		InitUploadDto initUploadDto = uploadFileService.init(bucket.getId(), pathname, totalMd5, totalSize, chunks);
		if (initUploadDto.isFileExists() && initUploadDto.getFileHouse() != null) {
			FileHouse fileHouse = initUploadDto.getFileHouse();
			log.info("秒传{},fileHouseId:{}", pathname, fileHouse.getId());
//			PageHelper.startPage(1, 1);
//			Resource resource = resourceService.findOneByFilter(new Filter("fileHouseId", Filter.Operator.eq, fileHouse.getId()));
//			Long thumbFileHouseId = null;
//			if(resource != null){
//				thumbFileHouseId = resource.getThumbFileHouseId();
//			}
			resourceService.addOrUpdateResource(pathname, lastModified, isPublic, contentType, cover, fileHouse, bucket, true);
		}
		initUploadDto.setFileHouse(null);

//		InitUploadDto initUploadDto = new InitUploadDto();
//		FileHouse fileHouse = fileHouseService.findByMd5AndSize(totalMd5, totalSize);
//		boolean md5Exists = fileHouse != null && fileHouse.getFileStatus() == FileHouse.FileStatus.OK;
//		initUploadDto.setFileExists(md5Exists);
//		if (md5Exists) {
//			log.info("秒传{},fileHouseId:{}", pathname, fileHouse.getId());
//			resourceService.addOrUpdateResource(pathname, lastModified, isPublic, contentType, cover, fileHouse, bucket, true);
//		} else {
//			boolean success = fileHouseService.initFileHouse(fileHouse, totalMd5, totalSize, chunks, pathname, initUploadDto);
//			if (!success) {
//				//初始化未成功，有可能是md5冲突了
//				fileHouse = fileHouseService.findByMd5AndSize(totalMd5, totalSize);
//				if (fileHouse != null) {
//					log.warn("fileHouse初始化失败，md5冲突，先把文件秒传，但有可能文件未上传成功，fileHouseId:{}", fileHouse.getId());
//					//先把文件关联到客户端
//					resourceService.addOrUpdateResource(pathname, lastModified, isPublic, contentType, cover, fileHouse, bucket, false);
//				}
//				//让客户端跳过上传
//				initUploadDto.setFileExists(true);
//			}
//		}
		return ResResult.success(initUploadDto);
	}
	
	@PostMapping("/upload/{bucketName}")
	@ApiOperation("上传文件")
	@OpenApi(perms = BucketPerm.INSERT)
	public ResResult upload(@PathVariable String bucketName,
							String pathname,
							MultipartFile file,
							String totalMd5,
							Long totalSize,
							String chunkMd5,
							Integer chunkIndex, Bucket bucket) throws Exception {
		Assert.notNull(chunkIndex, "chunkIndex不能为空");
//		fileHouseItemService.upload(bucket.getId(), fileHouse.getId(), chunkMd5, chunkIndex, file.getInputStream());
		uploadFileService.upload(bucket.getId(), pathname, chunkMd5, chunkIndex, file.getInputStream());
		return ResResult.success();
	}
	
	@PostMapping("/upload/mergeFiles")
	@OpenApi(perms = BucketPerm.INSERT)
	public ResResult mergeFiles(String bucketName,
								String totalMd5,
								Long totalSize,
								Integer chunks,
								@RequestParam(defaultValue = "false") Boolean isPublic,
								String contentType,
								String pathname,
								Long lastModified,
								@RequestParam(defaultValue = "false") Boolean updateMd5,
								@RequestParam(defaultValue = "false") Boolean wait,
								@RequestParam(defaultValue = "false") Boolean cover) throws ExecutionException, InterruptedException {
		Assert.notNull(totalMd5, "totalMd5不能为空");
		Assert.notNull(totalSize, "totalSize不能为空");
		Bucket bucket = bucketService.findOne("bucketName", bucketName);
		Assert.notNull(bucket, "bucket不存在:" + bucketName);
		auditService.writeRequestsRecord(bucket.getId(), 1);
		Future<FileHouse> future = uploadFileService.mergeFiles(bucket.getId(), pathname, updateMd5,chunks, fileHouse -> {
			resourceService.addOrUpdateResource(pathname, lastModified, isPublic, contentType, cover, fileHouse, bucket, true);
		});
		if (wait) {
			future.get();
		}
		return ResResult.success();
	}
	
}
