package mt.spring.mos.server.service;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.sdk.MosSdk;
import mt.spring.mos.sdk.entity.upload.UploadInfo;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @Author Martin
 * @Date 2020/11/21
 */
//@SpringBootTest(classes = ServerApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@RunWith(SpringRunner.class)
@Slf4j
public class TestResourceService {
	private String bucketName = "default";
	private MosSdk mosSdk;
	
	@Before
	public void setUp() {
		LoggingSystem.get(getClass().getClassLoader()).setLogLevel("root", LogLevel.INFO);
		LoggingSystem.get(getClass().getClassLoader()).setLogLevel("mt.spring", LogLevel.DEBUG);
		mosSdk = new MosSdk("http://localhost:9700", 5, bucketName, "b-T3wXaUu5umA3vumqEIVA==");
	}
	
	@Test
	public void testUpload() throws IOException, InterruptedException, IllegalAccessException {
//		File file = new File("H:\\out\\test\\t.mp4");
//		File file = new File("C:\\Users\\Administrator\\Desktop\\李茂涛java应聘材料\\20201019.pdf");
//		File file = new File("C:\\Users\\Administrator\\Desktop\\esxi密码.txt");
		File file = new File("C:\\Users\\Administrator\\Desktop\\ESXi-6.7-Custom.iso");
//		File file = new File("D:\\迅雷下载\\CentOS-7-x86_64-Minimal-2003.iso");
		String pathname = "test2/" + file.getName();
//		mosSdk.uploadStream(new ByteArrayInputStream(pathname.getBytes()), new UploadInfo("test2/test.txt", true));
		mosSdk.uploadStream(new FileInputStream(file), new UploadInfo(pathname, true));
	}
	
	@Test
	public void testM5() throws IOException {
		File file = new File("D:\\迅雷下载\\macOS Mojave 10.14.6 (18G103)_Torrentmac.net.dmg");
		String MD5 = DigestUtils.md5Hex(new FileInputStream(file));
		System.out.println(MD5);
	}
	
	@After
	public void after() {
		mosSdk.shutdown();
	}
}
