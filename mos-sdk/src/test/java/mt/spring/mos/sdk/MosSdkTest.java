package mt.spring.mos.sdk;

import mt.spring.mos.sdk.entity.DirAndResource;
import mt.spring.mos.sdk.entity.PageInfo;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
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
		long openId = 1L;
		String bucketName = "default";
		String pubKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCOpjLje-tBUCrb_lEdyVKRAOjxJmYGstI81sYzeo3O05dDbXNVO0jCFedeA7DVh_5G4RhaG1RtH-xLxaV03ibErujhndIh3BZctvB3W69AMLXjjYBxOltGNAGjsRHfeufOVpl2bVtlr61M3AWuNUX4I5slcYbyWhcp3zhL03ZMBwIDAQAB";
		String url = "http://localhost:9700";
		sdk = new MosSdk(url, openId, bucketName, pubKey);
	}
	
	@Test
	public void testUpload() throws IOException {
		sdk.upload("test.properties", new FileInputStream("C:\\Users\\Administrator\\Desktop\\mos\\server-1.0\\application.properties"));
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
//		RestTemplate restTemplate = new RestTemplate();
//		restTemplate.getForObject("",String.class);
		
		String url = sdk.getUrl("esxi密码.txt", 30L);
		System.out.println(url);
	}
	
}