package mt.spring.mos.client.controller;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.entity.ClientInfo;
import mt.spring.mos.base.entity.SpaceInfo;
import mt.spring.mos.client.entity.MosClientProperties;
import mt.spring.mos.client.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Arrays;
import java.util.List;

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
	@Autowired
	private ClientService clientService;
	
	@GetMapping("/info")
	public ClientInfo info() {
		List<MosClientProperties.BasePath> detailBasePaths = mosClientProperties.getDetailBasePaths();
		long totalSpace = 0;
		long freeSpace = 0;
		for (MosClientProperties.BasePath basePath : detailBasePaths) {
			File path = new File(basePath.getPath());
			totalSpace += path.getTotalSpace();
			freeSpace += path.getFreeSpace();
		}
		ClientInfo clientInfo = new ClientInfo();
		SpaceInfo spaceInfo = new SpaceInfo();
		spaceInfo.setFreeSpace(freeSpace);
		spaceInfo.setTotalSpace(totalSpace);
		clientInfo.setSpaceInfo(spaceInfo);
		clientInfo.setIsEnableAutoImport(mosClientProperties.isEnableAutoImport());
		clientInfo.setIsHealth(clientService.isHealth());
		clientInfo.setBasePaths(Arrays.asList(mosClientProperties.getBasePaths()));
		clientInfo.setServerHosts(Arrays.asList(mosClientProperties.getServerHosts()));
		return clientInfo;
	}

//	@GetMapping("/resources")
//	public List<Resource> resources() {
//		List<MosClientProperties.BasePath> detailBasePaths = mosClientProperties.getDetailBasePaths();
//		List<Resource> resources = new ArrayList<>();
//		for (MosClientProperties.BasePath basePath : detailBasePaths) {
//			File file = new File(basePath.getPath());
//			Collection<File> files = FileUtils.listFiles(file, null, true);
//			for (File file1 : files) {
//				String pathname = file1.getPath().replace(file.getPath(), "");
//				Resource resource = new Resource();
//				resource.setPathname(pathname);
//				resource.setSizeByte(FileUtils.sizeOf(file1));
//				resources.add(resource);
//			}
//		}
//		return resources;
//	}
	
}
