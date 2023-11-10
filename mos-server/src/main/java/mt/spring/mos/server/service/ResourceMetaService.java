package mt.spring.mos.server.service;

import lombok.extern.slf4j.Slf4j;
import mt.common.fragment.TaskFragment;
import mt.spring.mos.server.config.AsyncConfiguration;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Resource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2022/3/12
 */
@Service
@Slf4j
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
	private LockService lockService;
	@Autowired
	@Lazy
	private AccessControlService accessControlService;
	@Autowired
	private TaskFragment taskFragment;
	@Autowired
	@Lazy
	private DirService dirService;
	@Autowired
	@Lazy
	private BucketService bucketService;
	
	/**
	 * 资源属性计算
	 */
	@Async(AsyncConfiguration.META_EXECUTOR_NAME)
	public void calculateMeta(Bucket bucket, Long resourceId) {
		lockService.tryLock("resource-calculateMeta-" + resourceId, () -> {
			Resource resource = resourceService.findById(resourceId);
			if (resource == null) {
				return;
			}
			if (resource.getFileHouseId() == null) {
				return;
			}
			String pathname = resourceService.getPathname(resource);
			log.info("开始计算资源属性:{}", pathname);
			String videoLength = resource.getVideoLength();
			
			accessControlService.useMosSdk(0L, bucket.getBucketName(), mosSdk -> {
				//下载文件
				File tempDir = new File(FileUtils.getTempDirectory(), "thumb/" + resourceId);
				try {
					tempDir.mkdirs();
					File tempFile = new File(tempDir, resourceId + ".tmp");
					mosSdk.downloadFile(pathname, tempFile, true, mosServerProperties.getMetaNetWorkLimitSpeed() * 1024);
					
					if (resource.getThumbFileHouseId() == null) {
						//截图
						thumbService.createThumb(bucket.getId(), resource, tempDir, tempFile);
					}
					
					//视频长度
					if (StringUtils.isBlank(videoLength)) {
						videoService.setVideoInfo(resource, pathname, tempFile);
					}
				} finally {
					FileUtils.deleteQuietly(tempDir);
				}
				return null;
			});
		});
	}

//	@Autowired
//	private List<ThumbSupport> thumbSupports;
//
//	/**
//	 * 针对没有及时计算的文件，查漏补缺
//	 */
//	@Scheduled(fixedDelay = 300000)
//	public void refreshVideoMeta() {
//		List<Filter> filters = new ArrayList<>();
//		filters.add(new Filter("thumbFileHouseId", Filter.Operator.isNull));
//		filters.add(new Filter("thumbFails", Filter.Operator.lt, 3));
//		List<String> suffixs = thumbSupports.stream().flatMap(thumbSupport -> thumbSupport.getSuffixs().stream()).collect(Collectors.toList());
//		filters.add(new Filter("suffix", Filter.Operator.in, suffixs));
//		int pageSize = 100;
//		PageHelper.startPage(1, pageSize, "id desc");
//		List<Resource> list = resourceService.findByFilters(filters);
//		if (CollectionUtils.isEmpty(list)) {
//			return;
//		}
//		taskFragment.fragment(list, Resource::getId, resource -> {
//			Dir dir = dirService.findById(resource.getDirId());
//			Bucket bucket = bucketService.findById(dir.getBucketId());
//			calculateMeta(bucket, resource.getId());
//		});
//	}
	
}
