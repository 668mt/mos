package mt.spring.mos.server.controller.open;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import mt.common.entity.ResResult;
import mt.spring.mos.server.annotation.OpenApi;
import mt.spring.mos.server.config.aop.MosContext;
import mt.spring.mos.server.entity.BucketPerm;
import mt.spring.mos.server.entity.dto.InitUploadDto;
import mt.spring.mos.server.entity.po.*;
import mt.spring.mos.server.listener.ClientWorkLogEvent;
import mt.spring.mos.server.service.*;
import mt.utils.common.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

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
		org.springframework.util.Assert.notNull(bucket, "bucket不存在");
		auditService.doAudit(MosContext.getContext(), Audit.Type.WRITE, Audit.Action.initUpload);
		Resource findResource = resourceService.findResourceByPathnameAndBucketId(pathname, bucket.getId(), false);
		InitUploadDto initUploadDto = new InitUploadDto();
		FileHouse fileHouse = fileHouseService.findByMd5AndSize(totalMd5, totalSize);
		boolean md5Exists = fileHouse != null && fileHouse.getFileStatus() == FileHouse.FileStatus.OK;
		if (!cover) {
			Assert.isNull(findResource, "已存在相同的pathname");
		}
		if (md5Exists) {
			log.info("秒传{},fileHouseId:{}", pathname, fileHouse.getId());
			resourceService.addOrUpdateResource(pathname, lastModified, isPublic, contentType, cover, fileHouse, bucket);
		}
		
		if (fileHouse != null && fileHouse.getFileStatus() == FileHouse.FileStatus.UPLOADING) {
			//还未上传完成
			fileHouse.setChunks(chunks);
			fileHouse.setSizeByte(totalSize);
			fileHouseService.updateByIdSelective(fileHouse);
			
			FileHouseRelaClient fileHouseRelaClient = fileHouseRelaClientService.findUniqueFileHouseRelaClient(fileHouse.getId());
			Client client = clientService.findById(fileHouseRelaClient.getClientId());
			if (!clientService.isAlive(client)) {
				//原client不可用了，删掉原来的分片，重新找一个可用的client
				Client newClient = clientService.findRandomAvalibleClientForUpload(totalSize);
				log.info("{}:原client[{}]不可用，重新分配新的client[{}]", pathname, client.getName(), newClient.getName());
				//删掉原来上传的分片
				fileHouseItemService.deleteByFileHouseId(fileHouse.getId());
				//新增删除原分片的任务
				applicationEventPublisher.publishEvent(new ClientWorkLogEvent(this, ClientWorkLog.Action.DELETE_DIR, ClientWorkLog.ExeStatus.NOT_START, client.getId(), fileHouse.getChunkTempPath()));
				//设置新的client
				fileHouseRelaClient.setClientId(newClient.getId());
				fileHouseRelaClientService.updateById(fileHouseRelaClient);
			} else {
				List<FileHouseItem> items = fileHouseItemService.findList("fileHouseId", fileHouse.getId());
				if (items != null) {
					initUploadDto.setExistedChunkIndexs(items.stream().map(FileHouseItem::getChunkIndex).collect(Collectors.toList()));
				}
			}
		}
		
		initUploadDto.setFileExists(md5Exists);
		fileHouseService.getOrCreateFileHouse(totalMd5, totalSize, chunks);
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
							Integer chunkIndex) throws Exception {
		FileHouse fileHouse = fileHouseService.findByMd5AndSize(totalMd5, totalSize);
		Assert.notNull(fileHouse, "fileHouse不存在");
		Assert.notNull(chunkIndex, "chunkIndex不能为空");
		fileHouseItemService.upload(fileHouse.getId(), chunkMd5, chunkIndex, file.getInputStream());
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
		Assert.notNull(bucket, "bucket不存在");
		auditService.doAudit(MosContext.getContext(), Audit.Type.WRITE, Audit.Action.mergeFile);
		FileHouse fileHouse = fileHouseService.findByMd5AndSize(totalMd5, totalSize);
		if (chunks != null) {
			fileHouse.setChunks(chunks);
		}
		Future<FileHouse> future = fileHouseService.mergeFiles(fileHouse, updateMd5, (result) -> resourceService.addOrUpdateResource(pathname, lastModified, isPublic, contentType, cover, result, bucket));
		if (wait) {
			future.get();
		}
		return ResResult.success();
	}
	
}
