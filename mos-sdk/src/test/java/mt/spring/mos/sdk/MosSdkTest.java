package mt.spring.mos.sdk;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import mt.spring.mos.base.utils.IOUtils;
import mt.spring.mos.sdk.entity.DirAndResource;
import mt.spring.mos.sdk.entity.PageInfo;
import mt.spring.mos.sdk.entity.upload.UploadInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author Martin
 * @Date 2020/9/28
 */
public class MosSdkTest {
	
	private MosSdk sdk;
	
	@Before
	public void setUp() {
		long openId = 5;
		String bucketName = "default";
		String secretkey = "b-T3wXaUu5umA3vumqEIVA==";
		String url = "http://localhost:9700";
		sdk = new MosSdk(url, openId, bucketName, secretkey);
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		loggerContext.getLogger("root").setLevel(Level.INFO);
	}
	
	@Test
	public void testDeleteDir() throws IOException {
		System.out.println(sdk.deleteDir("test"));
	}
	
	@Test
	public void testInfo() throws IOException {
		String pathname = "/backup/test/dump-spider-202007292054.sql";
		System.out.println(sdk.isExists(pathname));
		System.out.println(sdk.getFileInfo(pathname));
		System.out.println(sdk.isFileModified("未标题-1+& - 副本.jpg",
				new File("C:\\Users\\Administrator\\Desktop\\李茂涛java应聘材料\\未标题-1+& - 副本.jpg")));
	}
	
	@Test
	public void testSplit() {
		IOUtils.SplitResult split = IOUtils.split(1000, 10, 500, 20);
		System.out.println(split);
		List<IOUtils.SplitPart> splitParts = split.getSplitParts();
		for (IOUtils.SplitPart splitPart : splitParts) {
			System.out.println(splitPart);
		}
	}
	
	@Test
	public void testDownload() throws Exception {
		PageInfo<DirAndResource> list = sdk.list("/backup/mc", null, 1, 5);
		ExecutorService executorService = Executors.newFixedThreadPool(5);
		String desPath = "C:\\Users\\Administrator\\Desktop\\test-recover\\新建";
//		MosUploadConfig mosUploadConfig = sdk.getMosUploadConfig();
//		mosUploadConfig.setMinPartSize(200 * MB);
		
		List<? extends Future<?>> collect = list.getList().stream()
				.filter(dirAndResource -> !dirAndResource.getIsDir())
				.map(dirAndResource -> {
					return executorService.submit(() -> {
						String path = dirAndResource.getPath();
						try {
							sdk.downloadFile(path, new File(desPath, dirAndResource.getFileName()), true);
						} catch (IOException e) {
							e.printStackTrace();
						}
					});
				}).collect(Collectors.toList());
		for (Future<?> future : collect) {
			future.get();
		}
		executorService.shutdownNow();
	}
	
	@Test
	public void testUpload() throws IOException {
//		File file = new File("G:\\work\\app\\mos-release\\server\\application.properties");
		File file = new File("C:\\Users\\Administrator\\Desktop\\test\\剑王朝-1.mp4");
		String pathname = file.getName();
		sdk.uploadFile(file, new UploadInfo(pathname, true, true));
		Assert.assertTrue(sdk.isExists(pathname));
		
		PageInfo<DirAndResource> list = sdk.list("/", null, null, null);
		Assert.assertTrue(list.getList().size() > 0);
		
		String url = sdk.getEncodedUrl(pathname, 30, TimeUnit.SECONDS);
		InputStream inputStream = new URL(url).openStream();
		Assert.assertNotNull(inputStream);
		File tempFile = new File("temp");
		if (sdk.isFileModified(pathname, tempFile)) {
			sdk.downloadFile(pathname, tempFile, true);
		}
		
		Assert.assertTrue(tempFile.isFile() && tempFile.exists());
		Assert.assertTrue(tempFile.delete());
		Assert.assertFalse(tempFile.exists());
		
		sdk.deleteFile(pathname);
		Assert.assertFalse(sdk.isExists(pathname));
	}
	
	@Test
	public void testConcurrent() throws Exception {
		sdk.deleteDir("/test");
		File file = new File("C:\\Users\\Administrator\\Desktop\\test");
		ExecutorService executorService = Executors.newFixedThreadPool(5);
		List<? extends Future<?>> collect = Stream.of(Objects.requireNonNull(file.listFiles()))
				.filter(File::isFile)
				.map(listFile -> executorService.submit(() -> {
					try {
						sdk.uploadFile(listFile, new UploadInfo("/test/" + listFile.getName(), true));
					} catch (IOException e) {
						e.printStackTrace();
					}
				})).collect(Collectors.toList());
		for (Future<?> future : collect) {
			future.get();
		}
		executorService.shutdownNow();
		System.out.println(collect.size());
	}
}