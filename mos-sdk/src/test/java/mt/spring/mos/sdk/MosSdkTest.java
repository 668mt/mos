package mt.spring.mos.sdk;

import mt.spring.mos.sdk.entity.DirAndResource;
import mt.spring.mos.sdk.entity.PageInfo;
import mt.spring.mos.sdk.entity.upload.UploadInfo;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2020/9/28
 */
public class MosSdkTest {
	
	private MosSdk sdk;
	
	@Before
	public void setUp() {
		LoggingSystem.get(MosSdkTest.class.getClassLoader()).setLogLevel("root", LogLevel.INFO);
		LoggingSystem.get(MosSdkTest.class.getClassLoader()).setLogLevel("mt.spring.mos", LogLevel.DEBUG);
		long openId = 5;
		String bucketName = "default";
		String secretkey = "b-T3wXaUu5umA3vumqEIVA==";
		String url = "http://localhost:9700";
		sdk = new MosSdk(url, openId, bucketName, secretkey);
	}
	
	@Test
	public void testUpload() throws IOException {
		sdk.uploadFile(new File("G:\\work\\app\\mos-release\\server\\application.properties"), new UploadInfo("application.properties", false));
		System.out.println("上传完成");
	}
	
	@Test
	public void testDelete() throws IOException {
		sdk.deleteFile("/test/测试+这是2&.txt");
//		sdk.deleteFile("/test");
		boolean exists = sdk.isExists("/test/测试+这是2&.txt");
		System.out.println(exists);
	}
	
	@Test
	public void testList() throws IOException {
		PageInfo<DirAndResource> list = sdk.list("/", null, null, null);
		List<DirAndResource> rows = list.getList();
		for (DirAndResource row : rows) {
			System.out.println(row.getIsDir() + "--" + row.getPath() + "--" + row.getFileName() + "--" + row.getLastModified());
		}
	}
	
	@Test
	public void getUrl() throws Exception {
		String pathanme = "/测试/脚本AASDASDDDF.sql";
		String url = sdk.getEncodedUrl(pathanme, 30, TimeUnit.SECONDS);
		System.out.println(url);
	}
	
	@Test
	public void testIsExists() throws IOException {
		boolean exists = sdk.isExists("/PS图21/test.jpg");
		System.out.println(exists);
	}
	
}