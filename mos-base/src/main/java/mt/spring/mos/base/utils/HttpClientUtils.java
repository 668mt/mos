//package mt.spring.mos.base.utils;
//
//import org.apache.http.Consts;
//import org.apache.http.HttpEntity;
//import org.apache.http.HttpHost;
//import org.apache.http.client.methods.CloseableHttpResponse;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.entity.ContentType;
//import org.apache.http.entity.mime.MultipartEntityBuilder;
//import org.apache.http.entity.mime.content.StringBody;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.message.BasicHttpRequest;
//import org.apache.tomcat.util.http.fileupload.IOUtils;
//import org.springframework.util.MultiValueMap;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.URL;
//import java.nio.charset.StandardCharsets;
//import java.util.List;
//import java.util.Map;
//
//public class HttpClientUtils {
//
//	public static CloseableHttpResponse httpClientUploadFile(CloseableHttpClient httpClient, String url, InputStream inputStream, Map<String, Object> params, long length) throws IOException {
//		HttpPost httpPost = new HttpPost(url);
//		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
//		ContentType contentType = ContentType.create("multipart/form-data", StandardCharsets.UTF_8);
//		builder.setContentType(contentType);
//		MyInputStreamBody file = new MyInputStreamBody(inputStream, contentType, "file", length);
//		builder.addPart("file", file);
//		for (Map.Entry<String, Object> stringObjectEntry : params.entrySet()) {
//			builder.addTextBody(stringObjectEntry.getKey(), stringObjectEntry.getValue() + "", contentType);
//		}
//		HttpEntity entity = builder.build();
//		httpPost.setEntity(entity);
//		try {
//			return httpClient.execute(httpPost);
//		} finally {
//			IOUtils.closeQuietly(inputStream);
//		}
//	}
//
//	public static CloseableHttpResponse postForm(CloseableHttpClient httpClient, String url, MultiValueMap<String, Object> params) throws IOException {
//		HttpPost httpPost = new HttpPost(url);
//		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
//		if (params != null) {
//			for (Map.Entry<String, List<Object>> stringListEntry : params.entrySet()) {
//				for (Object o : stringListEntry.getValue()) {
//					builder.addPart(stringListEntry.getKey(), new StringBody(o + "", ContentType.create("text/plain", Consts.UTF_8)));
//				}
//			}
//		}
//		HttpEntity entity = builder.build();
//		httpPost.setEntity(entity);
//		return httpClient.execute(httpPost);
//	}
//
//	public static CloseableHttpResponse get(CloseableHttpClient httpClient, String url) throws IOException {
//		BasicHttpRequest request = new BasicHttpRequest("GET", url);
//		return httpClient.execute(getHttpHost(new URL(url)), request);
//	}
//
//	public static CloseableHttpResponse delete(CloseableHttpClient httpClient, String url) throws IOException {
//		BasicHttpRequest request = new BasicHttpRequest("DELETE", url);
//		return httpClient.execute(getHttpHost(new URL(url)), request);
//	}
//
//	public static HttpHost getHttpHost(URL host) {
//		return new HttpHost(host.getHost(), host.getPort(), host.getProtocol());
//	}
//
//}