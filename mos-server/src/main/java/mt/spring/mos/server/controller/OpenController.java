package mt.spring.mos.server.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import mt.common.entity.ResResult;
import mt.spring.mos.server.annotation.OpenApi;
import mt.spring.mos.server.config.aop.MosContext;
import mt.spring.mos.server.entity.BucketPerm;
import mt.spring.mos.server.entity.dto.InitUploadDto;
import mt.spring.mos.server.entity.dto.ResourceSearchDto;
import mt.spring.mos.server.entity.po.*;
import mt.spring.mos.server.listener.ClientWorkLogEvent;
import mt.spring.mos.server.service.*;
import mt.spring.mos.server.service.resource.render.Content;
import mt.spring.mos.server.service.resource.render.ResourceRender;
import mt.spring.mos.server.utils.HttpClientServletUtils;
import mt.utils.common.Assert;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class OpenController implements InitializingBean {
	@Autowired
	private CloseableHttpClient httpClient;
	@Autowired
	private ResourceService resourceService;
	@Autowired
	private ClientService clientService;
	@Autowired
	private DirService dirService;
	@Autowired
	private BucketService bucketService;
	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;
	@Autowired
	private List<ResourceRender> renders;
	@Autowired
	private FileHouseService fileHouseService;
	@Autowired
	private FileHouseItemService fileHouseItemService;
	@Autowired
	private FileHouseRelaClientService fileHouseRelaClientService;
	@Autowired
	private AuditService auditService;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		renders.sort(Comparator.comparingInt(Ordered::getOrder));
	}
	
	@OpenApi(perms = BucketPerm.DELETE)
	@ApiOperation("删除文件")
	@DeleteMapping("/upload/{bucketName}/deleteFile")
	public ResResult deleteFile(String pathname, @PathVariable String bucketName, Bucket bucket) {
		resourceService.deleteResource(bucket, pathname);
		return ResResult.success(true);
	}
	
	@GetMapping("/upload/{bucketName}/isExists")
	@ApiOperation("判断文件是否存在")
	@OpenApi(perms = BucketPerm.SELECT)
	public ResResult isExists(String pathname, @PathVariable String bucketName, Bucket bucket) {
		auditService.doAudit(MosContext.getContext(), Audit.Type.READ, Audit.Action.isExists);
		Resource resource = resourceService.findResourceByPathnameAndBucketId(pathname, bucket.getId());
		return ResResult.success(resource != null);
	}
	
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
		Resource findResource = resourceService.findResourceByPathnameAndBucketId(pathname, bucket.getId());
		InitUploadDto initUploadDto = new InitUploadDto();
		FileHouse fileHouse = fileHouseService.findByMd5AndSize(totalMd5, totalSize);
		boolean md5Exists = fileHouse != null && fileHouse.getFileStatus() == FileHouse.FileStatus.OK;
		if (!cover) {
			org.springframework.util.Assert.state(findResource == null, "已存在相同的pathname");
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
	
	
	@PutMapping("/rename/{bucketName}")
	@ApiOperation("修改文件名")
	@OpenApi(perms = BucketPerm.UPDATE)
	public ResResult rename(@PathVariable String bucketName, String pathname, String desPathname) {
		Assert.notNull(pathname, "文件路径不能为空");
		Assert.notNull(desPathname, "目标文件路径不能为空");
		auditService.doAudit(MosContext.getContext(), Audit.Type.READ, Audit.Action.rename, pathname + "->" + desPathname);
		resourceService.rename(bucketName, pathname, desPathname);
		return ResResult.success();
	}
	
	@GetMapping("/mos/{bucketName}/**")
	@ApiOperation("获取资源")
	@OpenApi(pathnamePrefix = "/mos/{bucketName}", perms = BucketPerm.SELECT)
	public ModelAndView mos(@RequestParam(defaultValue = "false") Boolean thumb,
							@RequestParam(defaultValue = "false") Boolean download,
							@RequestParam(defaultValue = "true") Boolean render,
							@PathVariable String bucketName,
							HttpServletRequest request,
							HttpServletResponse httpServletResponse
	) throws Exception {
		String requestURI = request.getRequestURI();
		String pathname = requestURI.substring(("/mos/" + bucketName).length() + 1);
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		String originPathname = URLDecoder.decode(pathname, "UTF-8");
		Bucket bucket = bucketService.findOne("bucketName", bucketName);
		Assert.notNull(bucket, "bucket不存在");
		
		Resource resource = resourceService.findResourceByPathnameAndBucketId(originPathname, bucket.getId());
		Client client = clientService.findRandomAvalibleClientForVisit(resource, thumb);
		Assert.notNull(resource, "资源不存在");
		Assert.notNull(client, "资源不存在");
		String url = resourceService.getDesUrl(client, bucket, resource, thumb);
		String responseContentType = resource.getContentType();
		if (thumb) {
			//缩略图不走渲染
			render = false;
			responseContentType = "image/jpeg";
		} else {
			auditService.auditResourceVisits(resource.getId());
		}
		if(download){
			responseContentType = "application/octet-stream";
			render = false;
		}
		Audit audit = auditService.startAudit(MosContext.getContext(), Audit.Type.READ, Audit.Action.visit, thumb ? "缩略图" : null);
		if (render) {
			for (ResourceRender resourceRender : renders) {
				if (resourceRender.shouldRend(request, bucket, resource)) {
					return resourceRender.rend(new ModelAndView(), request, httpServletResponse, new Content(bucket, resource, client, url, audit));
				}
			}
		}
		
		if (StringUtils.isBlank(responseContentType)) {
			responseContentType = "application/octet-stream";
		}
		Map<String, String> headers = new HashMap<>();
		headers.put("content-type", responseContentType);
		HttpClientServletUtils.forward(httpClient, url, request, httpServletResponse, auditService.createAuditStream(httpServletResponse.getOutputStream(), audit), headers);
		return null;
	}
	
	@GetMapping("/list/{bucketName}/**")
	@ApiOperation("查询文件列表")
	@OpenApi(pathnamePrefix = "/list/{bucketName}", perms = BucketPerm.SELECT)
	public ResResult list(@PathVariable String bucketName, String keyWord, Integer pageNum, Integer pageSize, HttpServletRequest request) throws Exception {
		auditService.doAudit(MosContext.getContext(), Audit.Type.READ, Audit.Action.list);
		String requestURI = request.getRequestURI();
		String path = requestURI.substring(("/list/" + bucketName).length());
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		if (StringUtils.isBlank(path)) {
			path = "/";
		}
		Bucket bucket = bucketService.findOne("bucketName", bucketName);
		Assert.notNull(bucket, "bucket不存在");
		ResourceSearchDto resourceSearchDto = new ResourceSearchDto();
		resourceSearchDto.setKeyWord(keyWord);
		resourceSearchDto.setPageNum(pageNum);
		resourceSearchDto.setPageSize(pageSize);
		resourceSearchDto.setPath(path);
		return ResResult.success(resourceService.findDirAndResourceVoListPage(resourceSearchDto, bucket.getId()));
	}
	
}
