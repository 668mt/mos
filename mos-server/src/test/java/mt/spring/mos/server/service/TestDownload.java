package mt.spring.mos.server.service;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.utils.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @Author Martin
 * @Date 2023/5/13
 */
@Slf4j
public class TestDownload {
	public static void main(String[] args) throws IOException {
		LoggingSystem.get(TestDownload.class.getClassLoader()).setLogLevel("root", LogLevel.INFO);
		String srcUrl = "http://192.168.0.2:4100/mos/default/resources/1.mp4";
		HttpGet httpGet = new HttpGet(srcUrl);
		CloseableHttpResponse execute = HttpClients.createDefault().execute(httpGet);
		InputStream content = execute.getEntity().getContent();
		FileOutputStream fileOutputStream = new FileOutputStream("D:/test/download.mp4");
		IOUtils.copyLargeLimitSpeed(content, fileOutputStream, 1024 * 10);
	}
	
}
