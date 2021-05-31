package mt.spring.mos.server.service;

import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.server.dao.ResourceMapper;
import mt.spring.mos.server.entity.dto.Thumb;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.FileHouse;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.service.clientapi.ClientApiFactory;
import mt.spring.mos.server.service.clientapi.IClientApi;
import mt.spring.mos.server.service.thumb.ThumbSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

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
	
	@Async
	public Future<Boolean> createThumb(Long resourceId) {
		Resource resource = resourceService.findById(resourceId);
		if (resource == null) {
			return new AsyncResult<>(false);
		}
		String pathname = resourceService.getPathname(resource);
		if (resource.getThumbFileHouseId() != null) {
			log.warn("文件{}已经存在截图，跳过此次截图", pathname);
			return new AsyncResult<>(false);
		}
		if (resource.getFileHouseId() == null) {
			log.warn("文件{}无filehouseId，跳过此次截图", pathname);
			return new AsyncResult<>(false);
		}
		ThumbSupport thumbSupport = thumbSupports.stream().filter(t -> t.match(resource.getSuffix())).findFirst().orElse(null);
		if (thumbSupport == null) {
			log.warn("文件{}无截图生成器，跳过此次截图", pathname);
			return new AsyncResult<>(false);
		}
		try {
			log.info("生成{}截图", pathname);
			Client client = clientService.findRandomAvalibleClientForVisit(resource, false);
			FileHouse fileHouse = resourceService.findFileHouse(resource);
			Boolean encode = fileHouse.getEncode();
			String encodeKey = encode != null && encode ? fileHouse.getPathname() : null;
			IClientApi clientApi = clientApiFactory.getClientApi(client);
			Thumb thumb = clientApi.createThumb(fileHouse.getPathname(), encodeKey, thumbSupport.getSeconds(), thumbSupport.getWidth());
			FileHouse thumbFileHouse = new FileHouse();
			thumbFileHouse.setEncode(true);
			thumbFileHouse.setPathname(thumb.getPathname());
			thumbFileHouse.setFileStatus(FileHouse.FileStatus.OK);
			thumbFileHouse.setSizeByte(thumb.getSize());
			thumbFileHouse.setMd5(thumb.getMd5());
			thumbFileHouse.setChunks(1);
			thumbFileHouse = fileHouseService.createFileHouseIfNotExists(thumbFileHouse, client);
			resource.setThumbFileHouseId(thumbFileHouse.getId());
			resourceService.updateByIdSelective(resource);
			log.info("{}截图生成成功:{}", pathname, thumb);
			return new AsyncResult<>(true);
		} catch (RuntimeException e) {
			log.error(pathname + "截图失败：" + e.getMessage(), e);
			Integer thumbFails = resource.getThumbFails();
			thumbFails++;
			resource.setThumbFails(thumbFails);
			resourceService.updateByIdSelective(resource);
		}
		return new AsyncResult<>(false);
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
