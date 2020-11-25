package mt.spring.mos.sdk.http;

import com.alibaba.fastjson.JSONObject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.utils.MyInputStreamBody;
import mt.spring.mos.sdk.utils.Assert;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.util.EntityUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.util.MultiValueMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2020/11/23
 */
@Slf4j
public class ServiceClient {
	private CloseableHttpClient httpClient;
	private HttpClientConnectionManager connectionManager;
	
	@Setter
	public int socketTimeout = 3600 * 1000;
	@Setter
	public int connectionTimeout = 50 * 1000;
	@Setter
	public int connectionRequestTimeout = -1;
	
	public ServiceClient() {
		this.httpClient = newHttpClient();
	}
	
	static class DisabledValidationTrustManager implements X509TrustManager {
		DisabledValidationTrustManager() {
		}
		
		public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
		}
		
		public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
		}
		
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}
	
	public HttpClientConnectionManager newConnectionManager(boolean disableSslValidation, int maxTotalConnections, int maxConnectionsPerRoute, long timeToLive, TimeUnit timeUnit, RegistryBuilder registryBuilder) {
		if (registryBuilder == null) {
			registryBuilder = RegistryBuilder.create().register("http", PlainConnectionSocketFactory.INSTANCE);
		}
		
		if (disableSslValidation) {
			try {
				SSLContext sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null, new TrustManager[]{new DisabledValidationTrustManager()}, new SecureRandom());
				registryBuilder.register("https", new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE));
			} catch (NoSuchAlgorithmException | KeyManagementException var10) {
				log.warn("Error creating SSLContext", var10);
			}
		} else {
			registryBuilder.register("https", SSLConnectionSocketFactory.getSocketFactory());
		}
		
		Registry<ConnectionSocketFactory> registry = registryBuilder.build();
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry, null, null, null, timeToLive, timeUnit);
		connectionManager.setMaxTotal(maxTotalConnections);
		connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
		connectionManager.setValidateAfterInactivity(2 * 1000);
		return connectionManager;
	}
	
	public HttpClientConnectionManager newConnectionManager() {
		if (this.connectionManager == null) {
			synchronized (this) {
				if (this.connectionManager == null) {
					this.connectionManager = newConnectionManager(
							true,
							1024,
							1024,
							-1, TimeUnit.MILLISECONDS,
							null);
					new Timer().schedule(new TimerTask() {
						@Override
						public void run() {
							connectionManager.closeExpiredConnections();
						}
					}, 30000, 5000);
				}
			}
		}
		return connectionManager;
	}
	
	public CloseableHttpClient newHttpClient() {
		final RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(connectionRequestTimeout)
				.setSocketTimeout(socketTimeout)
				.setConnectTimeout(connectionTimeout)
				.setContentCompressionEnabled(false)
				.setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();
		return HttpClients.custom().setDefaultRequestConfig(requestConfig)
				.setConnectionManager(newConnectionManager()).disableRedirectHandling()
				.build();
	}
	
	public CloseableHttpClient getHttpClient() {
		if (httpClient == null) {
			synchronized (this) {
				if (httpClient == null) {
					httpClient = newHttpClient();
				}
			}
		}
		return httpClient;
	}
	
	public void shutdown() {
		if (connectionManager != null) {
			connectionManager.shutdown();
		}
	}
	
	
	public CloseableHttpResponse upload(String url, InputStream inputStream, Map<String, Object> params, long length) throws IOException {
		HttpPost httpPost = new HttpPost(url);
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		ContentType contentType = ContentType.create("multipart/form-data", StandardCharsets.UTF_8);
		builder.setContentType(contentType);
		builder.addPart("file", new MyInputStreamBody(inputStream, contentType, "file", length));
		for (Map.Entry<String, Object> stringObjectEntry : params.entrySet()) {
			builder.addTextBody(stringObjectEntry.getKey(), stringObjectEntry.getValue() + "", contentType);
		}
		HttpEntity entity = builder.build();
		httpPost.setEntity(entity);
		try {
			return getHttpClient().execute(httpPost);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}
	
	public CloseableHttpResponse postForm(String url, MultiValueMap<String, Object> params) throws IOException {
		HttpPost httpPost = new HttpPost(url);
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		if (params != null) {
			for (Map.Entry<String, List<Object>> stringListEntry : params.entrySet()) {
				for (Object o : stringListEntry.getValue()) {
					builder.addPart(stringListEntry.getKey(), new StringBody(o + "", ContentType.create("text/plain", Consts.UTF_8)));
				}
			}
		}
		HttpEntity entity = builder.build();
		httpPost.setEntity(entity);
		return getHttpClient().execute(httpPost);
	}
	
	public CloseableHttpResponse get(String url) throws IOException {
		BasicHttpRequest request = new BasicHttpRequest("GET", url);
		return getHttpClient().execute(getHttpHost(new URL(url)), request);
	}
	
	public <T> T get(String url, Class<T> type) throws IOException {
		return checkSuccessAndGetResult(get(url), type);
	}
	
	public CloseableHttpResponse post(String url, HttpEntity httpEntity) throws IOException {
		HttpPost httpPost = new HttpPost(url);
		httpPost.setEntity(httpEntity);
		return getHttpClient().execute(httpPost);
	}
	
	public CloseableHttpResponse delete(String url) throws IOException {
		BasicHttpRequest request = new BasicHttpRequest("DELETE", url);
		return getHttpClient().execute(getHttpHost(new URL(url)), request);
	}
	
	public HttpHost getHttpHost(URL host) {
		return new HttpHost(host.getHost(), host.getPort(), host.getProtocol());
	}
	
	public <T> T checkSuccessAndGetResult(CloseableHttpResponse response, Class<T> type) throws IOException {
		HttpEntity entity = response.getEntity();
		String s = EntityUtils.toString(entity, "UTF-8");
		log.debug("请求结果：{}", s);
		JSONObject result = JSONObject.parseObject(s);
		Assert.state("ok".equalsIgnoreCase(result.getString("status")), "请求失败：" + result.getString("message"));
		return result.getObject("result", type);
	}
}
