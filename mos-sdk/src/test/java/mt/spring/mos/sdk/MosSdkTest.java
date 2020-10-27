package mt.spring.mos.sdk;

import mt.spring.mos.sdk.entity.DirAndResource;
import mt.spring.mos.sdk.entity.PageInfo;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * @Author Martin
 * @Date 2020/9/28
 */
public class MosSdkTest {
	
	private MosSdk sdk;
	
	@Before
	public void setUp() {
		LoggingSystem.get(MosSdkTest.class.getClassLoader()).setLogLevel("root", LogLevel.INFO);
		long openId = 4L;
		String bucketName = "default";
		String pubKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCFqOMCrmmllNUHgCIPDMOk3X-YMAVQ4EA8E1srKkc51xe8dK5OQ1vnROdInxSqTLWaQFlyFICeJD4Sn2TLInTME-gTmD-D83fCeHQ7XccIfPE0nV9Ht6WYWORHlc9Qv-bk6AHMQiWwGzIwiHNUZWNblvyyyQdwqAqEJg0OGIdD3QIDAQAB";
		String url = "http://localhost:9700";
		sdk = new MosSdk(url, openId, bucketName, pubKey);
	}
	
	@Test
	public void testUpload() throws IOException {
//		sdk.upload("test.properties", new FileInputStream("C:\\Users\\Administrator\\Desktop\\mos\\server-1.0\\application.properties"), true);
		sdk.upload("/test/测试+这是2&.txt",
				new FileInputStream("G:\\test-upload\\10\\test.txt"),true);
		System.out.println("上传完成");
	}
	
	@Test
	public void testList() {
		PageInfo<DirAndResource> list = sdk.list("/test", null, null, null);
		List<DirAndResource> rows = list.getList();
		for (DirAndResource row : rows) {
			System.out.println(row.getIsDir() + "--" + row.getPath() + "--" + row.getFileName());
		}
	}
	
	@Test
	public void getUrl() throws Exception {
		String pathanme = "/测试/脚本AASDASDDDF.sql";
		String url = sdk.getEncodedUrl(pathanme, 30L);
		System.out.println(url);
	}
	
}