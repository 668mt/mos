package mt.spring.mos.server.service;

import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.server.dao.ResourceMapper;
import mt.spring.mos.server.entity.po.FileHouse;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.service.thumb.ThumbSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
	private FileHouseService fileHouseService;
	@Autowired
	private ResourceMapper resourceMapper;
	
	public void createThumb(Long bucketId, Resource resource, File tempDir, File tempFile) {
		String pathname = resourceService.getPathname(resource);
		if (resource.getThumbFileHouseId() != null) {
			log.warn("文件{}已经存在截图，跳过此次截图", pathname);
			return;
		}
		ThumbSupport thumbSupport = thumbSupports.stream().filter(t -> t.match(resource.getSuffix())).findFirst().orElse(null);
		if (thumbSupport == null) {
			log.warn("文件{}无截图生成器，跳过此次截图", pathname);
			return;
		}
		
		long start = System.currentTimeMillis();
		try {
			Long resourceId = resource.getId();
			log.info("开始生成{}截图,resourceId={}", pathname, resourceId);
			File thumbFile = thumbSupport.createThumb(resource, tempDir, tempFile);
			Assert.state(thumbFile != null && thumbFile.exists(), "截图失败:" + pathname);
			FileHouse thumbFileHouse = fileHouseService.uploadLocalFile(bucketId, thumbFile);
			resource.setThumbFileHouseId(thumbFileHouse.getId());
			resourceService.updateByIdSelective(resource);
			log.info("{}截图生成成功，用时：{}ms", pathname, System.currentTimeMillis() - start);
		} catch (Throwable e) {
			log.error("{}截图失败：{}，用时：{}ms", pathname, e.getMessage(), System.currentTimeMillis() - start, e);
			int thumbFails = resource.getThumbFails();
			thumbFails++;
			resource.setThumbFails(thumbFails);
			resourceService.updateByIdSelective(resource);
		}
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
