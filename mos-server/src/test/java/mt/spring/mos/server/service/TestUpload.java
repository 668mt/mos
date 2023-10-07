package mt.spring.mos.server.service;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.sdk.entity.upload.UploadInfo;
import mt.spring.mos.server.controller.admin.ManageController;
import mt.spring.mos.server.entity.po.FileHouse;
import mt.utils.common.CollectionUtils;
import mt.utils.common.TimeWatcher;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2023/9/8
 */
@Slf4j
public class TestUpload extends BaseSpringBootTest {
	
	@Autowired
	private ManageController manageController;
	@Autowired
	private FileHouseService fileHouseService;
	@Autowired
	private DirService dirService;
	
	private void clear() {
		dirService.realDeleteDir(bucket.getId(), "/test");
		
		List<FileHouse> notUsedFileHouseList = fileHouseService.findNotUsedFileHouseList(0);
		if (CollectionUtils.isEmpty(notUsedFileHouseList)) {
			log.info("没有要删除的文件！");
			return;
		}
		for (FileHouse fileHouse : notUsedFileHouseList) {
			fileHouseService.clearFileHouse(fileHouse.getId());
		}
	}
	
	@Test
	public void test() throws IOException {
		clear();
		
		ExecutorService executorService = Executors.newFixedThreadPool(50);
		TimeWatcher timeWatcher = new TimeWatcher();
		timeWatcher.start();
		try {
			String path = "D:/test/upload";
//			String path = "D:/test/upload2";
//			String path = "D:/test/upload3";
			AtomicInteger atomicInteger = new AtomicInteger();
			Collection<File> files = FileUtils.listFiles(new File(path), null, false);
			int total = files.size();
			List<? extends Future<?>> futures = files.stream().map(file -> executorService.submit(() -> {
				log.info("上传第{}/{}个文件：{}", atomicInteger.incrementAndGet(), total, file.getName());
				UploadInfo uploadInfo = new UploadInfo("/test/2023/09/" + file.getName()+".tmp", false);
//				UploadInfo uploadInfo = new UploadInfo("/test/2023/09/test.tmp", true);
				try {
					mosSdk.uploadFile(file, uploadInfo);
				} catch (Exception e) {
					log.error("{}上传错误,{}", file.getName(), e.getMessage(), e);
					throw new RuntimeException(e);
				}
			})).collect(Collectors.toList());
			for (Future<?> future : futures) {
				try {
					future.get();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		} finally {
			timeWatcher.recordFromStart("上传结束");
			executorService.shutdownNow();
		}
	}
}
