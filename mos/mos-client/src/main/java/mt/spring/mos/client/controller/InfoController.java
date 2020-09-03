package mt.spring.mos.client.controller;

import mt.spring.mos.client.entity.MosClientProperties;
import mt.spring.mos.client.entity.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.*;

/**
 * @Author Martin
 * @Date 2020/5/18
 */
@RestController
@RequestMapping("/client")
@Slf4j
public class InfoController {
	@Autowired
	private MosClientProperties mosClientProperties;
	
	@GetMapping("/info")
	public Map<String, Object> info() {
		String[] basePaths = mosClientProperties.getBasePaths();
		Assert.notNull(basePaths, "未配置basePaths");
		long totalSpace = 0;
		long freeSpace = 0;
		for (String basePath : basePaths) {
			File path = new File(basePath);
			totalSpace += path.getTotalSpace();
			freeSpace += path.getFreeSpace();
		}
		Map<String, Long> spaceInfo = new HashMap<>();
		spaceInfo.put("totalSpace", totalSpace);
		spaceInfo.put("freeSpace", freeSpace);
		Map<String, Object> info = new HashMap<>();
		info.put("spaceInfo", spaceInfo);
		info.put("isEnableAutoImport", mosClientProperties.isEnableAutoImport());
		return info;
	}
	
	@GetMapping("/resources")
	public List<Resource> resources() {
		String[] basePaths = mosClientProperties.getBasePaths();
		Assert.notNull(basePaths, "未配置basePaths");
		List<Resource> resources = new ArrayList<>();
		for (String basePath : basePaths) {
			File file = new File(basePath);
			Collection<File> files = FileUtils.listFiles(file, null, true);
			for (File file1 : files) {
				String pathname = file1.getPath().replace(file.getPath(), "");
				Resource resource = new Resource();
				resource.setPathname(pathname);
				resource.setSizeByte(FileUtils.sizeOf(file1));
				resources.add(resource);
			}
		}
		return resources;
	}
	
}
