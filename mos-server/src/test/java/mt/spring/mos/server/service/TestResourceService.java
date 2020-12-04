package mt.spring.mos.server.service;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.sdk.MosSdk;
import mt.spring.mos.sdk.entity.upload.UploadConfig;
import mt.spring.mos.sdk.entity.upload.UploadInfo;
import mt.spring.mos.base.stream.MosEncodeInputStream;
import mt.spring.mos.base.stream.MosEncodeOutputStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static mt.spring.mos.base.utils.IOUtils.MB;

/**
 * @Author Martin
 * @Date 2020/11/21
 */
//@SpringBootTest(classes = ServerApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@RunWith(SpringRunner.class)
@Slf4j
public class TestResourceService {
	private MosSdk mosSdk;
	
	@Before
	public void setUp() {
		LoggingSystem.get(getClass().getClassLoader()).setLogLevel("root", LogLevel.INFO);
		LoggingSystem.get(getClass().getClassLoader()).setLogLevel("mt.spring", LogLevel.DEBUG);
		mosSdk = new MosSdk("http://localhost:9700", 5, "default", "b-T3wXaUu5umA3vumqEIVA==");
//		mosSdk = new MosSdk("http://192.168.0.12:9700", 7, "mos", "nQRgTl93PRZhxftqj0WCQw==");
//		System.setProperty("mos.upload.threadPoolCore", "5");
	}
	
	@Test
	public void testUpload() throws IOException, InterruptedException, IllegalAccessException {
//		File file = new File("H:\\out\\test\\t.mp4");
//		File file = new File("C:\\Users\\Administrator\\Desktop\\李茂涛java应聘材料\\20201019.pdf");
//		File file = new File("C:\\Users\\Administrator\\Desktop\\esxi密码.txt");
//		File file = new File("C:\\Users\\Administrator\\Desktop\\ESXi-6.7-Custom.iso");
		File file = new File("H:\\movies\\剑王朝\\剑王朝-1.mp4");
		String pathname = "test2/" + file.getName();
//		mosSdk.uploadStream(new ByteArrayInputStream(pathname.getBytes()), new UploadInfo("test2/test.txt", true));
		mosSdk.uploadStream(new FileInputStream(file), new UploadInfo(pathname, true));
	}
	
	@Test
	public void testDownload() throws IOException, InterruptedException, IllegalAccessException {
		File file = new File("H:\\movies\\剑王朝\\剑王朝-1.mp4");
		String pathname = "test2/" + file.getName();
		mosSdk.downloadFile(pathname, new File("C:\\Users\\Administrator\\Desktop\\test\\" + file.getName()), true);
	}
	
	@Test
	public void testM5() throws IOException {
		File file = new File("D:\\迅雷下载\\macOS Mojave 10.14.6 (18G103)_Torrentmac.net.dmg");
		String MD5 = DigestUtils.md5Hex(new FileInputStream(file));
		System.out.println(MD5);
	}
	
	@Test
	public void writeEncodeFile() throws IOException {
		File file = new File("C:\\Users\\Administrator\\Desktop\\test\\C-VID_20201128_170029.mp4");
		String desFile = "C:\\Users\\Administrator\\Desktop\\test\\test-C-VID_20201128_170029.mp4";
		System.out.println(file.length());
		FileInputStream inputStream = new FileInputStream(file);
		String key = "asdaggasdasddczxcads";
		IOUtils.copy(inputStream, new MosEncodeOutputStream(new FileOutputStream(desFile), key));
	}
	
	@Test
	public void readEncodeFile() throws IOException {
		String key = "asdaggasdasddczxcads";
		File encodeFile = new File("C:\\Users\\Administrator\\Desktop\\test\\test-C-VID_20201128_170029.mp4");
		System.out.println(encodeFile.length());
		String desFile = "C:\\Users\\Administrator\\Desktop\\test\\test-decode-C-VID_20201128_170029.mp4";
		FileInputStream inputStream = new FileInputStream(encodeFile);
		MosEncodeInputStream mosEncodeInputStream = new MosEncodeInputStream(inputStream, key);
		System.out.println(mosEncodeInputStream.available());
		IOUtils.copy(mosEncodeInputStream, new FileOutputStream(desFile));
	}
	
	@After
	public void after() {
		mosSdk.shutdown();
	}
}
