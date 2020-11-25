package mt.spring.mos.client.service;

import mt.spring.mos.base.utils.IOUtils;
import mt.spring.mos.client.entity.MosClientProperties;
import mt.spring.mos.client.entity.dto.MergeFileDto;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Author Martin
 * @Date 2020/11/21
 */
public class TestClientService {
	private ClientService clientService;
	private String basePath;
	
	@Before
	public void setUp() throws Exception {
		basePath = "H:\\out\\test\\test-upload";
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
	public void testMerge() throws IOException {
		MergeFileDto mergeFileDto = new MergeFileDto();
		mergeFileDto.setPath("test");
		mergeFileDto.setChunks(8);
		mergeFileDto.setDesPathname("t.mp4");
		clientService.mergeFiles(mergeFileDto);
	}
}
