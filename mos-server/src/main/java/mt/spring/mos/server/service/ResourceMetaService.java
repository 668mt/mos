package mt.spring.mos.server.service;

import com.github.pagehelper.PageHelper;
import mt.common.tkmapper.Filter;
import mt.spring.mos.base.utils.CollectionUtils;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Dir;
import mt.spring.mos.server.entity.po.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2022/3/12
 */
@Service
public class ResourceMetaService {
	@Autowired
	private ThumbService thumbService;
	@Autowired
	private VideoService videoService;
	@Autowired
	private MosServerProperties mosServerProperties;
	@Autowired
	@Lazy
	private ResourceService resourceService;
	@Autowired
	@Lazy
	private DirService dirService;
	@Autowired
	@Lazy
	private BucketService bucketService;
	@Autowired
	private TaskScheduleService taskScheduleService;
	
	/**
	 * 资源属性计算
	 */
	@Async
	public void calculateMeta(Bucket bucket, Long resourceId) {
		//截图
		thumbService.createThumb(bucket, resourceId);
		//视频长度
		videoService.setVideoInfo(bucket, resourceId);
	}
	
	@Scheduled(fixedDelay = 30000)
	public void refreshAllThumb() {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("thumbFileHouseId", Filter.Operator.isNull));
		filters.add(new Filter("thumbFails", Filter.Operator.lt, 3));
		List<String> suffixs = mosServerProperties.getFileSuffix().get("video").stream().map(s -> s.startsWith(".") ? s : "." + s).collect(Collectors.toList());
		filters.add(new Filter("suffix", Filter.Operator.in, suffixs));
		int pageSize = 1000;
		PageHelper.startPage(1, pageSize, "id desc");
		List<Resource> list = resourceService.findByFilters(filters);
		if (CollectionUtils.isEmpty(list)) {
			return;
		}
		taskScheduleService.fragment(list, Resource::getId, resource -> {
			Dir dir = dirService.findById(resource.getDirId());
			Bucket bucket = bucketService.findById(dir.getBucketId());
			thumbService.createThumb(bucket, resource.getId());
		});
	}
	
	@Scheduled(fixedDelay = 30000)
	public void refreshAllVideoInfo() {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("videoLength", Filter.Operator.isNull));
		List<String> suffixs = mosServerProperties.getFileSuffix().get("video").stream().map(s -> s.startsWith(".") ? s : "." + s).collect(Collectors.toList());
		filters.add(new Filter("suffix", Filter.Operator.in, suffixs));
		int pageSize = 1000;
		PageHelper.startPage(1, pageSize, "id desc");
		List<Resource> list = resourceService.findByFilters(filters);
		if (CollectionUtils.isEmpty(list)) {
			return;
		}
		taskScheduleService.fragment(list, Resource::getId, resource -> {
			Dir dir = dirService.findById(resource.getDirId());
			Bucket bucket = bucketService.findById(dir.getBucketId());
			videoService.setVideoInfo(bucket, resource.getId());
		});
	}
}
