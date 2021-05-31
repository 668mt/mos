package mt.spring.mos.client.service;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.stream.BoundedInputStream;
import mt.spring.mos.base.stream.MosEncodeInputStream;
import mt.spring.mos.base.utils.FfmpegUtils;
import mt.spring.mos.client.entity.MosClientProperties;
import mt.spring.mos.client.entity.dto.MergeFileDto;
import mt.spring.mos.client.entity.dto.Thumb;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import ws.schild.jave.MultimediaObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;

/**
 * @Author Martin
 * @Date 2020/11/21
 */
@Slf4j
public class TestClientService {
	private ClientService clientService;
	private String basePath;
	
	@Before
	public void setUp() throws Exception {
//		basePath = "H:\\out\\test\\test-upload";
		basePath = "D:/test-upload";
		clientService = new ClientService();
		MosClientProperties mosClientProperties = new MosClientProperties();
		mosClientProperties.setBasePaths(new String[]{basePath});
		ReflectionTestUtils.setField(clientService, "mosClientProperties", mosClientProperties);
		clientService.afterPropertiesSet();
	}

//	@Test
//	public void testUpload() throws IOException {
//		File file = new File("H:\\out\\test\\t.mp4");
//		List<InputStream> inputStreams = IOUtils.splitToStreams(file, IOUtils.MB);
//		for (int i = 0; i < inputStreams.size(); i++) {
//			InputStream inputStream = inputStreams.get(i);
//			clientService.upload(inputStream, "/test/part" + i, inputStream.available());
//		}
//	}
	
	@Test
	public void testMerge() throws Exception {
		MergeFileDto mergeFileDto = new MergeFileDto();
		mergeFileDto.setPath("test");
		mergeFileDto.setChunks(8);
		mergeFileDto.setDesPathname("t.mp4");
		clientService.mergeFiles(mergeFileDto);
	}
	
	@Test
	public void testMd5() throws UnsupportedEncodingException {
		String desPathname = "/10/mc/txt/test/未标题-1+& - 副本.jpg";
		String url = "http://localhost:9800/client/md5?pathname=" + desPathname;
		RestTemplate restTemplate = new RestTemplate();
		JSONObject forObject = restTemplate.getForObject(url, JSONObject.class);
		System.out.println(forObject);
	}
	
	@Test
	public void testThumb() throws Exception {
		String pathname = "test/segment_067.ts";
		Thumb thumb = clientService.addThumb(pathname, 400, 0, pathname);
		System.out.println(thumb);
	}
	
	@Test
	public void testTemp() throws Exception {
		String pathname = "202011/28bf87e35d4c15950650638638e22156";
		String encodeKey = "/202011/28bf87e35d4c15950650638638e22156";
		File file = new File(basePath, pathname);
		int tempMb = 50;
		BoundedInputStream boundedInputStream = new BoundedInputStream(new MosEncodeInputStream(new FileInputStream(file), encodeKey), file.length());
		File tempFile = new File(file.getParent(), "test-temp.mp4");
		if (tempFile.exists()) {
			tempFile.delete();
		}
		log.info("创建{}M临时文件{}", tempMb, tempFile);
		FileOutputStream outputStream = new FileOutputStream(tempFile);
		IOUtils.copy(boundedInputStream, outputStream);
	}
	
	@Test
	public void testFfmpeg() throws Exception {
//		String pathname = "/202011/9daa3be1-80ae-4ba4-ac71-ebc38243ccfc";
		String pathname = "/202011/test.jpg";
		File srcFile = new File(basePath, pathname);
		MultimediaObject object = new MultimediaObject(srcFile);
		System.out.println(object.getInfo());
		FfmpegUtils.screenShot(srcFile, new File(basePath, "202011/test-thumb.jpg"), 100, 0);
	}
	
	@Test
	public void testDecode() throws Exception {
		String path = "C:\\Users\\Administrator\\Downloads\\111948.mp4";
		String desFile = "C:\\Users\\Administrator\\Downloads\\out.mp4";
		String encodeKey = "/202006/464011e53a6ef44dde57d466caa05029";
		MosEncodeInputStream mosEncodeInputStream = new MosEncodeInputStream(new FileInputStream(path), encodeKey);
		IOUtils.copy(mosEncodeInputStream, new FileOutputStream(desFile));
	}
}
