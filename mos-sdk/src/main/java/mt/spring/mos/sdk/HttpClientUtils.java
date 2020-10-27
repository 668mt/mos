package mt.spring.mos.sdk;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHttpRequest;
import org.apache.tomcat.util.http.fileupload.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpClientUtils {
	
	public static CloseableHttpResponse httpClientUploadFiles(CloseableHttpClient httpClient, String url, InputStream[] inputStreams, String[] pathnames) throws IOException {
		HttpPost httpPost = new HttpPost(url);
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		ContentType contentType = ContentType.create("multipart/form-data", StandardCharsets.UTF_8);
		builder.setContentType(contentType);
		for (InputStream inputStream : inputStreams) {
			builder.addBinaryBody("files", inputStream, contentType, "files");// 文件流
		}
		for (String pathname : pathnames) {
			builder.addTextBody("pathnames", pathname, contentType);// 类似浏览器表单提交，对应input的name和value
		}
		HttpEntity entity = builder.build();
		httpPost.setEntity(entity);
		try {
			return httpClient.execute(httpPost);
		} finally {
			for (InputStream inputStream : inputStreams) {
				IOUtils.closeQuietly(inputStream);
			}
		}
	}
	
	public static CloseableHttpResponse get(CloseableHttpClient httpClient, String url) throws IOException {
		BasicHttpRequest request = new BasicHttpRequest("GET", url);
		return httpClient.execute(getHttpHost(new URL(url)), request);
	}
	
	public static CloseableHttpResponse delete(CloseableHttpClient httpClient, String url) throws IOException {
		BasicHttpRequest request = new BasicHttpRequest("DELETE", url);
		return httpClient.execute(getHttpHost(new URL(url)), request);
	}
	
	public static HttpHost getHttpHost(URL host) {
		return new HttpHost(host.getHost(), host.getPort(), host.getProtocol());
	}
	
}