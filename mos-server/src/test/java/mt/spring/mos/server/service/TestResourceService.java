package mt.spring.mos.server.service;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import mt.common.entity.ResResult;
import mt.spring.mos.base.stream.MosEncodeInputStream;
import mt.spring.mos.base.stream.MosEncodeOutputStream;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.sdk.MosSdk;
import mt.spring.mos.sdk.entity.DirAndResource;
import mt.spring.mos.sdk.entity.PageInfo;
import mt.spring.mos.sdk.entity.upload.UploadInfo;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @Author Martin
 * @Date 2020/11/21
 */
@Slf4j
public class TestResourceService {
	private MosSdk mosSdk;
	
	@Before
	public void setUp() {
		LoggingSystem.get(getClass().getClassLoader()).setLogLevel("root", LogLevel.INFO);
		LoggingSystem.get(getClass().getClassLoader()).setLogLevel("mt.spring", LogLevel.DEBUG);
		mosSdk = new MosSdk("http://localhost:9700", 5, "default", "b-T3wXaUu5umA3vumqEIVA==");
	}
	
	@Test
	public void testList() throws IOException {
		PageInfo<DirAndResource> list = mosSdk.list("/", null, 1, 1);
		System.out.println(list.getList().size());
	}
	
	@Test
	public void testUpload() throws IOException, InterruptedException, IllegalAccessException {
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
	
	@Test
	public void testAddDir() throws IOException, ExecutionException, InterruptedException {
		String dir = "/mc/210520/test";
		ExecutorService executorService = Executors.newFixedThreadPool(10);
		String cookie = "JSESSIONID.3ca41a8f=node0qxunc33c9s16pr7muy4t60jc1.node0; currentMember=9999; remember-me=YWRtaW46MTYyMjQ2MzY0Njk2Mjo3NTM1N2Q4NzkxM2U1OGZhZjdlNTdhMDFiZWEwZWFmOTBiZDk1NmVlMjQ2MTZlY2QyMTA2MWI3MWJkYTg1ZTQ1; screenResolution=1920x1080; jenkins-timestamper-offset=-28800000; JSESSIONID.3603a09e=node0r6xkh97qlofs1x9j90pav5gtp5.node0; SESSION=YTNlODBjNTItZmJjNS00ZWY5LTlhYzAtZTZhMGJmNTUxY2U0";
		RestTemplate restTemplate = new RestTemplate();
		String url = "http://localhost:9700/member/dir/default";
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("path", dir);
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("Cookie", cookie);
		
		for (int i = 0; i < 10; i++) {
			mosSdk.deleteDir("/mc");
			List<? extends Future<?>> collect = IntStream.range(0, 10)
					.mapToObj(index -> executorService.submit(() -> {
						ResResult resResult = restTemplate.postForObject(url, new HttpEntity<>(jsonObject, httpHeaders), ResResult.class);
						Assert.state(resResult != null && resResult.isSuccess(), "错误：" + resResult.getMessage());
					}))
					.collect(Collectors.toList());
			for (Future<?> future : collect) {
				future.get();
			}
		}
	}
	
	@After
	public void after() {
		mosSdk.shutdown();
	}
	
}
