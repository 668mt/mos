package mt.spring.mos.server.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import mt.common.entity.ResResult;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.annotation.OpenApi;
import mt.spring.mos.server.dao.RelaClientResourceMapper;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.dto.InitUploadDto;
import mt.spring.mos.server.entity.po.*;
import mt.spring.mos.server.listener.ClientWorkLogEvent;
import mt.spring.mos.server.service.*;
import mt.spring.mos.server.service.resource.render.ResourceRender;
import mt.spring.mos.server.utils.HttpClientServletUtils;
import mt.utils.Assert;
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
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static mt.common.tkmapper.Filter.Operator.eq;

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
	private MosServerProperties mosServerProperties;
	@Autowired
	private DirService dirService;
	@Autowired
	private BucketService bucketService;
	@Autowired
	private RelaClientResourceMapper relaClientResourceMapper;
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
	
	@Override
	public void afterPropertiesSet() throws Exception {
		renders.sort(Comparator.comparingInt(Ordered::getOrder));
	}
	
	@OpenApi
	@ApiOperation("删除文件")
	@DeleteMapping("/upload/{bucketName}/deleteFile")
	public ResResult deleteFile(String pathname, @PathVariable String bucketName, Bucket bucket) {
		try {
			resourceService.deleteResource(bucket, pathname);
			return ResResult.success(true);
		} catch (Exception e) {
			log.error(e.getMessage());
			ResResult result = ResResult.success(false);
			result.setMessage(e.getMessage());
			return result;
		}
	}
	
	@GetMapping("/upload/{bucketName}/isExists")
	@ApiOperation("判断文件是否存在")
	@OpenApi
	public ResResult isExists(String pathname, @PathVariable String bucketName, Bucket bucket) {
		Resource resource = resourceService.findResourceByPathnameAndBucketId(pathname, bucket.getId());
		return ResResult.success(resource != null);
	}
	
	@PostMapping("/upload/{bucketName}/init")
	@ApiOperation("上传初始化")
	@OpenApi
	public ResResult initUpload(@RequestParam(defaultValue = "false") Boolean isPublic,
								String contentType,
								@PathVariable String bucketName,
								String pathname,
								String totalMd5,
								Long totalSize,
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
		Resource findResource = resourceService.findResourceByPathnameAndBucketId(pathname, bucket.getId());
		InitUploadDto initUploadDto = new InitUploadDto();
		FileHouse fileHouse = fileHouseService.findByMd5AndSize(totalMd5, totalSize);
		boolean md5Exists = fileHouse != null && fileHouse.getFileStatus() == FileHouse.FileStatus.OK;
		if (!cover) {
			org.springframework.util.Assert.state(findResource == null, "已存在相同的pathname");
		}
		if (md5Exists) {
			log.info("秒传{},fileHouseId:{}", pathname, fileHouse.getId());
			resourceService.addOrUpdateResource(pathname, isPublic, contentType, cover, fileHouse, bucket);
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
				log.info("{}:原client[{}]不可用，重新分配新的client[{}]", pathname, client.getClientId(), newClient.getClientId());
				//删掉原来上传的分片
				fileHouseItemService.deleteByFileHouseId(fileHouse.getId());
				//新增删除原分片的任务
				applicationEventPublisher.publishEvent(new ClientWorkLogEvent(this, ClientWorkLog.Action.DELETE_DIR, ClientWorkLog.ExeStatus.NOT_START, client.getClientId(), fileHouse.getChunkTempPath()));
				//设置新的client
				fileHouseRelaClient.setClientId(newClient.getClientId());
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
	@OpenApi
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
	public ResResult mergeFiles(String bucketName,
								String totalMd5,
								Long totalSize,
								Integer chunks,
								@RequestParam(defaultValue = "false") Boolean isPublic,
								String contentType,
								String pathname,
								@RequestParam(defaultValue = "false") Boolean updateMd5,
								@RequestParam(defaultValue = "false") Boolean wait,
								@RequestParam(defaultValue = "false") Boolean cover) throws ExecutionException, InterruptedException {
		Assert.notNull(totalMd5, "totalMd5不能为空");
		Assert.notNull(totalSize, "totalSize不能为空");
		Bucket bucket = bucketService.findOne("bucketName", bucketName);
		Assert.notNull(bucket, "bucket不存在");
		FileHouse fileHouse = fileHouseService.findByMd5AndSize(totalMd5, totalSize);
		if (chunks != null) {
			fileHouse.setChunks(chunks);
		}
		Future<FileHouse> future = fileHouseService.mergeFiles(fileHouse, updateMd5, (result) -> resourceService.addOrUpdateResource(pathname, isPublic, contentType, cover, result, bucket));
		if (wait) {
			future.get();
		}
		return ResResult.success();
	}
	
	
	@PutMapping("/rename/{bucketName}")
	@ApiOperation("修改文件名")
	@OpenApi
	public ResResult rename(@PathVariable String bucketName, String pathname, String desPathname) {
		Assert.notNull(pathname, "文件路径不能为空");
		Assert.notNull(desPathname, "目标文件路径不能为空");
		resourceService.rename(bucketName, pathname, desPathname);
		return ResResult.success();
	}
	
	@GetMapping("/mos/{bucketName}/**")
	@ApiOperation("获取资源")
	@OpenApi(pathnamePrefix = "/mos/{bucketName}")
	public ModelAndView mos(@PathVariable String bucketName, HttpServletRequest request, HttpServletResponse httpServletResponse, @RequestParam(defaultValue = "false") Boolean download) throws Exception {
		String requestURI = request.getRequestURI();
		String pathname = requestURI.substring(("/mos/" + bucketName).length() + 1);
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		String originPathname = URLDecoder.decode(pathname, "UTF-8");
		Bucket bucket = bucketService.findOne("bucketName", bucketName);
		Assert.notNull(bucket, "bucket不存在");
		
		Client client = clientService.findRandomAvalibleClientForVisit(bucket.getId(), originPathname);
		Resource resource = resourceService.findResourceByPathnameAndBucketId(originPathname, bucket.getId());
		Assert.notNull(resource, "资源不存在");
		Assert.notNull(client, "资源不存在");
		String desPathname = resourceService.getDesPathname(bucket, resource);
		String url = client.getUrl() + "/mos" + desPathname;
		if (download) {
			String responseContentType = "application/octet-stream";
			Map<String, String> headers = new HashMap<>();
			headers.put("content-type", responseContentType);
			HttpClientServletUtils.forward(httpClient, url, request, httpServletResponse, headers);
		} else {
			for (ResourceRender render : renders) {
				if (render.shouldRend(request, bucket, resource)) {
					return render.rend(new ModelAndView(), request, httpServletResponse, bucket, resource, client, url);
				}
			}
			throw new IllegalStateException("资源没有相关的渲染器");
		}
		return null;
	}
	
	@GetMapping("/list/{bucketName}/**")
	@ApiOperation("查询文件列表")
	@OpenApi(pathnamePrefix = "/list/{bucketName}")
	public ResResult list(@PathVariable String bucketName, String keyWord, Integer pageNum, Integer pageSize, HttpServletRequest request) throws Exception {
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
		
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("path", eq, URLDecoder.decode(path, "UTF-8")));
		filters.add(new Filter("bucketId", eq, bucket.getId()));
		Dir dir = dirService.findOneByFilters(filters);
		Assert.notNull(dir, "路径不存在");
		return ResResult.success(resourceService.findDirAndResourceVoListPage(keyWord, pageNum, pageSize, bucket.getId(), dir.getId()));
	}
	
}
