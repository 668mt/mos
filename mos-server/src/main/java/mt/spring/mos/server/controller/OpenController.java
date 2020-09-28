package mt.spring.mos.server.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import mt.common.entity.ResResult;
import mt.common.tkmapper.Filter;
import mt.spring.mos.sdk.HttpClientUtils;
import mt.spring.mos.sdk.ProcessInputStream;
import mt.spring.mos.server.annotation.OpenApi;
import mt.spring.mos.server.config.upload.UploadService;
import mt.spring.mos.server.config.upload.UploadTotalProcess;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.Dir;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.service.BucketService;
import mt.spring.mos.server.service.ClientService;
import mt.spring.mos.server.service.DirService;
import mt.spring.mos.server.service.ResourceService;
import mt.utils.Assert;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import static mt.common.tkmapper.Filter.Operator.eq;

/**
 * @Author Martin
 * @Date 2020/5/15
 */
@RestController
@RequestMapping("/")
@Api(tags = "开放接口")
@Slf4j
public class OpenController {
	@Autowired
	private CloseableHttpClient httpClient;
	@Autowired
	private ResourceService resourceService;
	@Autowired
	private ClientService clientService;
	@Autowired
	private MosServerProperties mosServerProperties;
	@Autowired
	private UploadService uploadService;
	@Autowired
	private DirService dirService;
	@Autowired
	private BucketService bucketService;
	
	@GetMapping("/upload/progress/reset")
	@OpenApi
	@ApiOperation("重置进度")
	public ResResult resetProcess(String uploadId, @RequestParam(defaultValue = "1") Integer taskCount) {
		UploadTotalProcess uploadTotalProcess = new UploadTotalProcess()
				.addUpProcess(UploadTotalProcess.MULTIPART_UPLOAD_NAME, 0.8);
		double weight = BigDecimal.valueOf(0.2).divide(BigDecimal.valueOf(taskCount), 2, RoundingMode.HALF_UP).doubleValue();
		for (int i = 0; i < taskCount; i++) {
			uploadTotalProcess.addUpProcess("item-" + i, weight);
		}
		uploadService.setUploadTotalProcess(uploadId, uploadTotalProcess);
		return ResResult.success();
	}
	
	@GetMapping("/upload/progress")
	@ApiOperation("查询上传进度")
	@OpenApi
	public ResResult getUploadIngress(String uploadId) {
		UploadTotalProcess uploadTotalProcess = uploadService.getUploadTotalProcess(uploadId);
		return ResResult.success(uploadTotalProcess.getPercent());
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
	
	@PostMapping("/upload/{bucketName}")
	@ApiOperation("上传文件")
	@OpenApi
	public ResResult upload(String uploadId, MultipartFile[] files, String[] pathnames, @PathVariable String bucketName) throws Exception {
		Assert.notNull(files, "上传文件不能为空");
		Assert.notNull(pathnames, "pathname不能为空");
		for (int i = 0; i < files.length; i++) {
			MultipartFile file = files[i];
			String pathname = pathnames[i];
			Assert.notBlank(pathname, "pathname不能为空");
			pathname = pathname.replace("\\", "/");
			if (!pathname.startsWith("/")) {
				pathname = "/" + pathname;
			}
			log.info("上传{}文件：{}", bucketName, pathname);
			Bucket bucket = bucketService.findOne("bucketName", bucketName);
			Assert.notNull(bucket, "bucket不存在");
			Assert.state(resourceService.findResourceByPathnameAndBucketId(pathname, bucket.getId()) == null, "已存在相同的pathname");
			BigDecimal minAvaliableSpaceGB = mosServerProperties.getMinAvaliableSpaceGB();
			long minSpace = minAvaliableSpaceGB.multiply(BigDecimal.valueOf(1024L * 1024 * 1024)).longValue();
			Client client = clientService.findRandomAvalibleClient(null, Math.max(minSpace, file.getSize() * 2));
			Assert.notNull(client, "无可用资源服务器");
			UploadTotalProcess uploadTotalProcess = uploadService.getUploadTotalProcess(uploadId);
			int index = i;
			resourceService.upload(client, new ProcessInputStream(file.getInputStream(), percent -> {
				uploadTotalProcess.updateProcess("item-" + index, percent);
				uploadService.setUploadTotalProcess(uploadId, uploadTotalProcess);
			}), file.getSize(), pathname, bucket);
			Resource resource = new Resource();
			resource.setPathname(pathname);
			resource.setSizeByte(file.getSize());
			resourceService.addResourceIfNotExist(resource, client.getClientId(), bucket.getId());
		}
		return ResResult.success();
	}
	
	@GetMapping("/mos/{bucketName}/**")
	@ApiOperation("获取资源")
	@OpenApi(pathnamePrefix = "/mos/{bucketName}")
	public void mos(@PathVariable String bucketName, HttpServletRequest request, HttpServletResponse httpServletResponse, Bucket bucket) throws Exception {
		String requestURI = request.getRequestURI();
		String pathname = requestURI.substring(("/mos/" + bucketName).length() + 1);
		String desPathname = resourceService.getDesPathname(bucket, pathname);
		Client client = clientService.findRandomAvalibleClient(URLDecoder.decode(pathname, "UTF-8"), 0);
		HttpClientUtils.forward(httpClient, "http://" + client.getIp() + ":" + client.getPort() + "/mos" + desPathname, request, httpServletResponse);
	}
	
	@GetMapping("/list/{bucketName}/**")
	@ApiOperation("查询文件列表")
	@OpenApi(pathnamePrefix = "/list/{bucketName}")
	public ResResult list(@PathVariable String bucketName, String keyWord, Integer pageNum, Integer pageSize, HttpServletRequest request) throws UnsupportedEncodingException {
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
