package mt.spring.mos.sdk;

import mt.spring.mos.sdk.entity.DirAndResource;
import mt.spring.mos.sdk.entity.PageInfo;
import mt.spring.mos.sdk.entity.upload.UploadInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

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
	public void testUpload() throws IOException {
//		File file = new File("G:\\work\\app\\mos-release\\server\\application.properties");
		File file = new File("C:\\Users\\Administrator\\Desktop\\test\\CatchHistoryMapper - 副本 - 副本.xml");
		String pathname = file.getName();
		sdk.uploadFile(file, new UploadInfo(pathname, true));
		Assert.assertTrue(sdk.isExists(pathname));
		
		PageInfo<DirAndResource> list = sdk.list("/", null, null, null);
		Assert.assertTrue(list.getList().size() > 0);
		
		String url = sdk.getEncodedUrl(pathname, 30, TimeUnit.SECONDS);
		InputStream inputStream = new URL(url).openStream();
		Assert.assertNotNull(inputStream);
		File tempFile = new File("temp");
		sdk.downloadFile(pathname, tempFile, true);
		
		Assert.assertTrue(tempFile.isFile() && tempFile.exists());
		tempFile.delete();
		Assert.assertFalse(tempFile.exists());
		
		sdk.deleteFile(pathname);
		Assert.assertFalse(sdk.isExists(pathname));
	}
}