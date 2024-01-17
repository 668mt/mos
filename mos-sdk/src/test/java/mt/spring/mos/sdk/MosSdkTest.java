package mt.spring.mos.sdk;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import mt.spring.mos.base.utils.IOUtils;
import mt.spring.mos.sdk.entity.DirAndResource;
import mt.spring.mos.sdk.entity.MosConfig;
import mt.spring.mos.sdk.entity.PageInfo;
import mt.spring.mos.sdk.entity.upload.UploadInfo;
import mt.spring.mos.sdk.http.ServiceClient;
import mt.spring.mos.sdk.utils.MosEncrypt;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
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
		MosConfig mosConfig = new MosConfig(url, bucketName, secretkey, openId);
		sdk = new MosSdk(mosConfig);
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		loggerContext.getLogger("root").setLevel(Level.DEBUG);
//		loggerContext.getLogger("root").setLevel(Level.INFO);
	}
	
	@Test
	public void testDeleteDir() throws IOException {
		System.out.println(sdk.deleteDir("test"));
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
		File file = new File("C:\\Users\\Administrator\\Downloads\\index.m3u8");
		String pathname = file.getName();
		sdk.uploadFile(file, new UploadInfo(pathname, true));
		Assert.assertTrue(sdk.isExists(pathname));
		
		PageInfo<DirAndResource> list = sdk.list("/", null, null, null);
		Assert.assertTrue(list.getList().size() > 0);
		
		String url = sdk.getUrl(pathname, 30, TimeUnit.SECONDS);
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
	public void testUpload2() throws IOException {
		File file = new File("D:\\softwares\\apache-maven-3.2.5\\lib", "maven-model-builder.license");
		sdk.uploadFile(file, new UploadInfo("/mc/202105/05683be8-0e6b-47eb-aed2-a01691884084/maven-model-builder.license", true));
	}
	
	@Test
	public void testConcurrent() throws Exception {
		sdk.deleteDir("/mc");
		testDeleteNotUseFileHouse();
		String dir = "/mc/2022/" + UUID.randomUUID().toString();
		File file = new File("D:\\softwares\\apache-maven-3.2.5\\lib");
		ExecutorService executorService = Executors.newFixedThreadPool(5);
		List<File> files = Stream.of(Objects.requireNonNull(file.listFiles()))
			.filter(File::isFile)
			.collect(Collectors.toList());
		List<Future<?>> futures = new ArrayList<>();
		for (File file1 : files) {
			futures.add(executorService.submit(() -> {
				try {
					sdk.uploadFile(file1, new UploadInfo(dir + "/" + file1.getName(), true));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}));
//			if (futures.size() > 10) {
//				break;
//			}
		}
		for (Future<?> future : futures) {
			future.get();
		}
		executorService.shutdownNow();
		System.out.println(futures.size());
	}
	
	@Test
	public void testDeleteNotUseFileHouse() throws IOException {
		String url = "http://localhost:9700/admin/deleteNotUsedFile/0";
		String cookie = "jenkins-timestamper-offset=-28800000; screenResolution=1920x1080; JSESSIONID.438c2a32=node01fprab1d8u0vb5gqalyrhpzdp4.node0; remember-me=MURxJTJGT3VCUCUyQjNkNzlja1czUHgwUHclM0QlM0Q6b0pGR01tZk9WYktzSXNaSTNicUNHQSUzRCUzRA; SESSION=ZDI3OWQ3MjYtOWI0OC00ZGZhLWE1MDYtMGFjNzUwMzIwYjRk";
		ServiceClient serviceClient = new ServiceClient(new MosConfig());
		BasicHttpRequest request = new BasicHttpRequest("DELETE", url);
		request.setHeader("Cookie", cookie);
		CloseableHttpResponse response = serviceClient.getHttpClient().execute(serviceClient.getHttpHost(new URL(url)), request);
		System.out.println(EntityUtils.toString(response.getEntity()));
	}
	
	@Test
	public void testConcurrent2() throws Exception {
		for (int i = 0; i < 10; i++) {
			testConcurrent();
		}
	}
	
	@Test
	public void testUrl() {
		String pathname = "@thumb@:/test/1.mp4";
		String url = sdk.getUrl(pathname, 1, TimeUnit.HOURS);
		System.out.println(url);
	}
	
	@Test
	public void testSign() throws Exception {
		MosEncrypt.MosEncryptContent decrypt = MosEncrypt.decrypt("b-T3wXaUu5umA3vumqEIVA==", "k7fTkYCSdbxpqZkdhCknjYOcmEdmtKdhwfj-4tDmpa5AFmkdIB__JmQwCDr7Oj0XtL1O3ewpzdpaUlvByscFqGejg==");
		long expireSeconds = decrypt.getExpireSeconds();
		Date date = new Date(decrypt.getSignTime());
		System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date));
		System.out.println(expireSeconds);
	}
}