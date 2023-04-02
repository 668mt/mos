package mt.spring.mos.server.service;

import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.base.utils.FfmpegUtils;
import mt.spring.mos.sdk.MosSdk;
import mt.spring.mos.server.dao.ResourceMapper;
import mt.spring.mos.server.entity.dto.Thumb;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.FileHouse;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.service.clientapi.ClientApiFactory;
import mt.spring.mos.server.service.clientapi.IClientApi;
import mt.spring.mos.server.service.thumb.ThumbSupport;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2021/4/21
 */
@Service
@Slf4j
public class ThumbService {
	@Autowired
	@Lazy
	private ResourceService resourceService;
	@Autowired
	private List<ThumbSupport> thumbSupports;
	@Autowired
	private ClientService clientService;
	@Autowired
	private ClientApiFactory clientApiFactory;
	@Autowired
	private FileHouseService fileHouseService;
	@Autowired
	private ResourceMapper resourceMapper;
	@Autowired
	@Lazy
	private AccessControlService accessControlService;
	
	public Boolean createThumb(Bucket bucket, Long resourceId) {
		Resource resource = resourceService.findById(resourceId);
		if (resource == null) {
			return false;
		}
		String pathname = resourceService.getPathname(resource);
		if (resource.getThumbFileHouseId() != null) {
			log.warn("文件{}已经存在截图，跳过此次截图", pathname);
			return false;
		}
		if (resource.getFileHouseId() == null) {
			log.warn("文件{}无filehouseId，跳过此次截图", pathname);
			return false;
		}
		ThumbSupport thumbSupport = thumbSupports.stream().filter(t -> t.match(resource.getSuffix())).findFirst().orElse(null);
		if (thumbSupport == null) {
			log.warn("文件{}无截图生成器，跳过此次截图", pathname);
			return false;
		}
		File tempFile = new File(FileUtils.getTempDirectory(), UUID.randomUUID().toString());
		try {
			log.info("生成{}截图", pathname);
			MosSdk mosSdk = accessControlService.getMosSdk(0L, bucket.getBucketName());
			String url = mosSdk.getUrl(pathname, 2, TimeUnit.HOURS);
			FfmpegUtils.screenShot(new URL(url), tempFile, thumbSupport.getWidth(), thumbSupport.getSeconds());
			Assert.state(tempFile.exists(), "截图失败:" + pathname);
			FileHouse thumbFileHouse = fileHouseService.uploadLocalFile(bucket.getId(),tempFile);
			resource.setThumbFileHouseId(thumbFileHouse.getId());
			resourceService.updateByIdSelective(resource);
			log.info("{}截图生成成功", pathname);
			return true;
		} catch (Exception e) {
			log.error(pathname + "截图失败：" + e.getMessage(), e);
			Integer thumbFails = resource.getThumbFails();
			thumbFails++;
			resource.setThumbFails(thumbFails);
			resourceService.updateByIdSelective(resource);
		} finally {
			FileUtils.deleteQuietly(tempFile);
		}
		return false;
	}
	
	public List<Resource> findNeedGenerateThumb(int limit) {
		PageHelper.startPage(1, limit);
		List<String> suffixs = new ArrayList<>();
		for (ThumbSupport thumbSupport : thumbSupports) {
			suffixs.addAll(thumbSupport.getSuffixs());
		}
		return resourceMapper.findNeedGenerateThumb(suffixs);
	}
}
