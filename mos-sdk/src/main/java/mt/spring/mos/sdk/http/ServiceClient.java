package mt.spring.mos.sdk.http;

import com.alibaba.fastjson.JSONObject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.stream.MyInputStreamBody;
import mt.spring.mos.base.utils.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
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
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.util.EntityUtils;

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
	private ResponseErrorHandler responseErrorHandler;
	
	@Setter
	public int socketTimeout = 3600 * 1000;
	@Setter
	public int connectionTimeout = 50 * 1000;
	@Setter
	public int connectionRequestTimeout = -1;
	private Timer timer;
	
	public ServiceClient() {
		this.httpClient = newHttpClient();
		this.responseErrorHandler = new DefaultResponseErrorHandler();
	}
	
	static class DisabledValidationTrustManager implements X509TrustManager {
		DisabledValidationTrustManager() {
		}
		
		@Override
		public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
		}
		
		@Override
		public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
		}
		
		@Override
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
					timer = new Timer();
					timer.schedule(new TimerTask() {
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
		if (httpClient != null) {
			try {
				httpClient.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}
	
	
	public CloseableHttpResponse upload(String url, InputStream inputStream, Map<String, Object> params, long length) throws IOException {
		ContentType contentType = ContentType.create("multipart/form-data", StandardCharsets.UTF_8);
		RequestBuilder requestBuilder = RequestBuilder.create()
				.setUrl(url)
				.setContentType(Request.ContentType.APPLICATION_FORM_DATA)
				.addBody("file", new MyInputStreamBody(inputStream, contentType, "file", length));
		for (Map.Entry<String, Object> stringObjectEntry : params.entrySet()) {
			requestBuilder.addBody(stringObjectEntry.getKey(), stringObjectEntry.getValue().toString());
		}
		try {
			return execute(requestBuilder.build());
		} finally {
			IOUtils.close(inputStream);
		}
	}
	
	private CloseableHttpResponse handleResponse(CloseableHttpResponse response) {
		if (responseErrorHandler != null && responseErrorHandler.hasError(response)) {
			responseErrorHandler.handError(response);
		}
		return response;
	}
	
	public CloseableHttpResponse get(String url, Header... headers) throws IOException {
		BasicHttpRequest request = new BasicHttpRequest("GET", url);
		if (headers != null) {
			request.setHeaders(headers);
		}
		return handleResponse(getHttpClient().execute(getHttpHost(new URL(url)), request));
	}
	
	public <T> T get(String url, Class<T> type) throws IOException {
		return checkSuccessAndGetResult(get(url), type);
	}
	
	public CloseableHttpResponse post(String url, HttpEntity httpEntity) throws IOException {
		HttpPost httpPost = new HttpPost(url);
		httpPost.setEntity(httpEntity);
		return handleResponse(getHttpClient().execute(httpPost));
	}
	
	public CloseableHttpResponse delete(String url) throws IOException {
		BasicHttpRequest request = new BasicHttpRequest("DELETE", url);
		return handleResponse(getHttpClient().execute(getHttpHost(new URL(url)), request));
	}
	
	public HttpHost getHttpHost(URL host) {
		return new HttpHost(host.getHost(), host.getPort(), host.getProtocol());
	}
	
	public <T> T checkSuccessAndGetResult(CloseableHttpResponse response, Class<T> type) throws IOException {
		HttpEntity entity = response.getEntity();
		String s = EntityUtils.toString(entity, "UTF-8");
		log.trace("请求结果：{}", s);
		JSONObject result = JSONObject.parseObject(s);
		Assert.state("ok".equalsIgnoreCase(result.getString("status")), "请求失败：" + result.getString("message"));
		return result.getObject("result", type);
	}
	
	public CloseableHttpResponse execute(Request request) throws IOException {
		return handleResponse(getHttpClient().execute(HttpHost.create(request.getUrl()), request.buildRequest()));
	}
}
