package mt.spring.mos.sdk.http;

import com.alibaba.fastjson.JSONObject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.stream.MyInputStreamBody;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.sdk.entity.MosConfig;
import okhttp3.Dispatcher;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.io.IOUtils;

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
import java.util.concurrent.TimeUnit;

/**
 * 基于 okhttp 的 HTTP 客户端
 *
 * @Author Martin
 * @Date 2020/11/23
 */
@Slf4j
public class ServiceClient {
	private volatile OkHttpClient httpClient;
	@Setter
	private ResponseErrorHandler responseErrorHandler;
	
	/**
	 * 读超时（毫秒）
	 */
	@Setter
	public int socketTimeout = 3600 * 1000;
	/**
	 * 连接超时（毫秒）
	 */
	@Setter
	public int connectionTimeout = 5000;
	/**
	 * 从连接池取连接的等待时间（毫秒）；okhttp 没有对应机制，保留仅为兼容
	 */
	@Setter
	public int connectionRequestTimeout = 5000;
	private final MosConfig mosConfig;
	
	public ServiceClient(MosConfig mosConfig) {
		this.mosConfig = mosConfig;
		this.httpClient = newHttpClient();
		this.responseErrorHandler = new DefaultResponseErrorHandler();
	}
	
	/**
	 * 不校验证书（与原 httpclient 实现保持一致）
	 */
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
			return new X509Certificate[0];
		}
	}
	
	public OkHttpClient newHttpClient() {
		Dispatcher dispatcher = new Dispatcher();
		dispatcher.setMaxRequests(Math.max(1, mosConfig.getMaxTotalConnections()));
		dispatcher.setMaxRequestsPerHost(Math.max(1, mosConfig.getMaxConnectionsPerRoute()));
		
		OkHttpClient.Builder builder = new OkHttpClient.Builder()
			.dispatcher(dispatcher)
			.connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
			.readTimeout(socketTimeout, TimeUnit.MILLISECONDS)
			.writeTimeout(socketTimeout, TimeUnit.MILLISECONDS)
			//不自动跟随重定向
			.followRedirects(false)
			.followSslRedirects(false)
			//不自动解压，也不让服务端压缩（与原 httpclient 的 contentCompressionEnabled(false) 行为一致）
			.addInterceptor(chain -> {
				okhttp3.Request request = chain.request();
				if (request.header("Accept-Encoding") == null) {
					request = request.newBuilder().header("Accept-Encoding", "identity").build();
				}
				return chain.proceed(request);
			});
		
		try {
			X509TrustManager trustManager = new DisabledValidationTrustManager();
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
			builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
			builder.hostnameVerifier((hostname, session) -> true);
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			log.warn("Error creating SSLContext", e);
		}
		return builder.build();
	}
	
	public OkHttpClient getHttpClient() {
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
		if (httpClient != null) {
			OkHttpClient client = httpClient;
			httpClient = null;
			client.dispatcher().executorService().shutdown();
			client.connectionPool().evictAll();
			if (client.cache() != null) {
				try {
					client.cache().close();
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			}
		}
	}
	
	/**
	 * multipart 上传
	 *
	 * @param url        地址
	 * @param inputStream 文件流
	 * @param params     其它表单参数
	 * @param length     文件长度
	 */
	public Response upload(String url, InputStream inputStream, Map<String, Object> params, long length) throws IOException {
		MediaType contentType = MediaType.parse("multipart/form-data; charset=utf-8");
		MultipartBody.Builder bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
		bodyBuilder.addFormDataPart("file", "file", new MyInputStreamBody(inputStream, contentType, "file", length));
		for (Map.Entry<String, Object> entry : params.entrySet()) {
			bodyBuilder.addFormDataPart(entry.getKey(), entry.getValue().toString());
		}
		okhttp3.Request request = new okhttp3.Request.Builder().url(url).post(bodyBuilder.build()).build();
		try {
			return execute(request);
		} finally {
			IOUtils.close(inputStream);
		}
	}
	
	private Response handleResponse(Response response) {
		if (responseErrorHandler != null && responseErrorHandler.hasError(response)) {
			responseErrorHandler.handError(response);
		}
		return response;
	}
	
	/**
	 * GET 请求
	 *
	 * @param url 地址
	 */
	public Response get(String url) throws IOException {
		return get(url, (Headers) null);
	}
	
	/**
	 * GET 请求
	 *
	 * @param url     地址
	 * @param headers 请求头
	 */
	public Response get(String url, Headers headers) throws IOException {
		okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(url).get();
		if (headers != null) {
			builder.headers(headers);
		}
		return handleResponse(execute(builder.build()));
	}
	
	public <T> T get(String url, Class<T> type) throws IOException {
		try (Response response = get(url)) {
			return checkSuccessAndGetResult(response, type);
		}
	}
	
	public Response post(String url, RequestBody requestBody) throws IOException {
		okhttp3.Request request = new okhttp3.Request.Builder().url(url).post(requestBody).build();
		return handleResponse(execute(request));
	}
	
	public <T> T postJson(String url, Object body, Class<T> type) throws IOException {
		MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
		RequestBody requestBody = RequestBody.create(JSONObject.toJSONString(body), mediaType);
		try (Response response = post(url, requestBody)) {
			return checkSuccessAndGetResult(response, type);
		}
	}
	
	public Response delete(String url) throws IOException {
		okhttp3.Request request = new okhttp3.Request.Builder().url(url).delete().build();
		return handleResponse(execute(request));
	}
	
	public HttpUrl getHttpUrl(URL host) {
		return HttpUrl.get(host);
	}
	
	public <T> T checkSuccessAndGetResult(Response response, Class<T> type) throws IOException {
		ResponseBody body = response.body();
		String s = body != null ? body.string() : "";
		log.trace("请求结果：{}", s);
		JSONObject result = JSONObject.parseObject(s);
		Assert.state("ok".equalsIgnoreCase(result.getString("status")), "请求失败：" + result.getString("message"));
		return result.getObject("result", type);
	}
	
	/**
	 * 执行请求
	 *
	 * @param request okhttp 请求
	 */
	public Response execute(okhttp3.Request request) throws IOException {
		return handleResponse(getHttpClient().newCall(request).execute());
	}
}
