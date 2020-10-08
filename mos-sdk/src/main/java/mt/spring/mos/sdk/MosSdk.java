package mt.spring.mos.sdk;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.sdk.entity.DirAndResource;
import mt.spring.mos.sdk.entity.PageInfo;
import mt.spring.mos.sdk.utils.Assert;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.HttpConnectionFactory;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2020/5/20
 */
@Data
@Slf4j
public class MosSdk {
	private String host;
	private String publicKey;
	private String bucketName;
	private Long openId;
	private CloseableHttpClient httpClient;
	
	class DisabledValidationTrustManager implements X509TrustManager {
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
				sslContext.init((KeyManager[])null, new TrustManager[]{new DisabledValidationTrustManager()}, new SecureRandom());
				registryBuilder.register("https", new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE));
			} catch (NoSuchAlgorithmException var10) {
				log.warn("Error creating SSLContext", var10);
			} catch (KeyManagementException var11) {
				log.warn("Error creating SSLContext", var11);
			}
		} else {
			registryBuilder.register("https", SSLConnectionSocketFactory.getSocketFactory());
		}
		
		Registry<ConnectionSocketFactory> registry = registryBuilder.build();
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry, (HttpConnectionFactory)null, (SchemePortResolver)null, (DnsResolver)null, timeToLive, timeUnit);
		connectionManager.setMaxTotal(maxTotalConnections);
		connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
		return connectionManager;
	}
	
	public HttpClientConnectionManager newConnectionManager() {
		HttpClientConnectionManager connectionManager = newConnectionManager(
				true,
				200,
				200,
				-1, TimeUnit.MILLISECONDS,
				null);
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				connectionManager.closeExpiredConnections();
			}
		}, 30000, 5000);
		return connectionManager;
	}
	
	public CloseableHttpClient newHttpClient() {
		final RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(5000)
				.setSocketTimeout(3600000)
				.setConnectTimeout(5000)
				.setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();
		return HttpClients.custom().setDefaultRequestConfig(requestConfig)
				.setConnectionManager(newConnectionManager()).disableRedirectHandling()
				.build();
	}
	
	public MosSdk(String host, Long openId, String bucketName, String publicKey) {
		if (host.endsWith("/")) {
			host = host.substring(0, host.length() - 1);
		}
		this.host = host;
		this.openId = openId;
		this.bucketName = bucketName;
		this.publicKey = publicKey;
	}
	
	/**
	 * 获取签名
	 *
	 * @param pathname      文件路径名
	 * @param expireSeconds 有效时间
	 * @return
	 */
	public String getSign(@NotNull String pathname, @Nullable Long expireSeconds) {
		JSONObject sign = new JSONObject();
		sign.put("pathname", pathname);
		sign.put("bucketName", bucketName);
		sign.put("expireSeconds", expireSeconds);
		sign.put("signTime", System.currentTimeMillis());
		try {
			String str = sign.toJSONString();
			String encrypt = RSAUtils.encryptLarge(str, publicKey);
			log.debug("{} 签名结果：{}", pathname, encrypt);
			return encrypt;
		} catch (Exception e) {
			throw new RuntimeException("加签失败：" + e.getMessage(), e);
		}
	}
	
	private CloseableHttpClient getHttpClient() {
		if (httpClient == null) {
			httpClient = newHttpClient();
		}
		return httpClient;
	}
	
	/**
	 * 获取访问地址
	 *
	 * @param pathname      文件路径名
	 * @param expireSeconds 有效时间
	 * @return
	 */
	public String getUrl(@NotNull String pathname, @Nullable Long expireSeconds) {
		return getUrl(pathname, expireSeconds, this.host);
	}
	
	public String getUrl(@NotNull String pathname, @Nullable Long expireSeconds, String host) {
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		String sign = getSign(pathname, expireSeconds);
		try {
			return host +
					"/mos/" +
					bucketName +
					pathname +
					"?sign=" +
					sign +
					"&openId=" +
					openId;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public interface UploadProcessListener {
		void update(double percent);
	}
	
	/**
	 * 带进度条的上传文件
	 *
	 * @param pathname
	 * @param inputStream
	 * @param uploadProcessListener
	 * @throws IOException
	 */
	public void upload(@NotNull String pathname, InputStream inputStream, UploadProcessListener uploadProcessListener) throws IOException {
		HttpClientUtils.get(getHttpClient(), host + "/upload/ingress/reset?" + getSignQueryParams(pathname, 30L));
		double[] percents = new double[]{0, 0};
		Timer timer = new Timer(true);
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					CloseableHttpResponse response = HttpClientUtils.get(getHttpClient(), host + "/upload/ingress?" + getSignQueryParams(pathname, 30L));
					Double result = getResult(response, Double.class);
					if (result == null) {
						result = 0d;
					}
					if (percents[1] != result) {
						percents[1] = result;
						uploadProcessListener.update(BigDecimal.valueOf(percents[0] * 0.7 + percents[1] * 0.3).setScale(2, RoundingMode.HALF_UP).doubleValue());
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, 2000, 500);
		ProcessInputStream processInputStream = new ProcessInputStream(inputStream, percent -> {
			if (percents[0] != percent) {
				percents[0] = percent;
				uploadProcessListener.update(BigDecimal.valueOf(percents[0] * 0.7 + percents[1] * 0.3).setScale(2, RoundingMode.HALF_UP).doubleValue());
			}
		});
		try {
			upload(pathname, processInputStream);
		} finally {
			timer.cancel();
		}
	}
	
	/**
	 * 上传文件
	 *
	 * @param pathname    文件路径名
	 * @param inputStream 文件流
	 * @throws IOException
	 */
	public void upload(@NotNull String pathname, InputStream inputStream) throws IOException {
		try {
			log.info("mos开始上传：{}", pathname);
			String sign = getSign(pathname, 3600L);
			CloseableHttpResponse response = HttpClientUtils.httpClientUploadFiles(getHttpClient(), host + "/upload/" + bucketName + "?openId=" + openId + "&sign=" + URLEncoder.encode(sign, "UTF-8"), new InputStream[]{inputStream}, new String[]{pathname});
			String s = EntityUtils.toString(response.getEntity());
			log.info("mos上传结果：{}", s);
			JSONObject jsonObject = JSONObject.parseObject(s);
			Assert.state("ok".equalsIgnoreCase(jsonObject.getString("status")), "上传失败:" + jsonObject.getString("message"));
		} finally {
			if (inputStream != null) {
				IOUtils.closeQuietly(inputStream);
			}
		}
	}
	
	private String getSignQueryParams(String pathname, Long expireSeconds) throws UnsupportedEncodingException {
		String sign = getSign(pathname, expireSeconds);
		pathname = URLEncoder.encode(pathname, "UTF-8");
		sign = URLEncoder.encode(sign, "UTF-8");
		return "sign=" + sign + "&openId=" + openId + "&pathname=" + pathname + "&bucketName=" + bucketName;
	}
	
	private <T> T getResult(CloseableHttpResponse response, Class<T> type) throws IOException {
		HttpEntity entity = response.getEntity();
		String s = EntityUtils.toString(entity, "UTF-8");
		log.debug("请求结果：{}", s);
		JSONObject result = JSONObject.parseObject(s);
		Assert.state("ok".equalsIgnoreCase(result.getString("status")), "访问资源服务器失败：" + result.getString("message"));
		return result.getObject("result", type);
	}
	
	/**
	 * 判断文件是否存在
	 *
	 * @param pathname 文件路径
	 * @return 文件是否存在
	 */
	public boolean isExists(@NotNull String pathname) throws IOException {
		CloseableHttpResponse closeableHttpResponse = HttpClientUtils.get(getHttpClient(), host + "/upload/" + bucketName + "/isExists?" + getSignQueryParams(pathname, 30L));
		return getResult(closeableHttpResponse, Boolean.class);
	}
	
	/**
	 * 删除文件
	 *
	 * @param pathname 文件路径
	 * @return 删除结果
	 */
	public boolean deleteFile(@NotNull String pathname) throws IOException {
		log.info("删除文件：{}", pathname);
		CloseableHttpResponse closeableHttpResponse = HttpClientUtils.delete(getHttpClient(), host + "/upload/" + bucketName + "/deleteFile?" + getSignQueryParams(pathname, 30L));
		HttpEntity entity = closeableHttpResponse.getEntity();
		String result = EntityUtils.toString(entity, "UTF-8");
		log.info("删除结果：{}", result);
		Assert.notNull(result, "请求资源服务器失败");
		JSONObject jsonObject = JSONObject.parseObject(result);
		return jsonObject.getBoolean("result");
	}
	
	public PageInfo<DirAndResource> list(@NotNull String path, @Nullable String keyWord, @Nullable Integer pageNum, @Nullable Integer pageSize) {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		log.info("查询文件列表:{}", path);
		String url = host +
				"/list/" +
				bucketName +
				path +
				"?sign=" +
				getSign(path, 30L) +
				"&openId=" +
				openId;
		if (StringUtils.isNotBlank(keyWord)) {
			url += "&keyWord=" + keyWord;
		}
		if (pageNum != null) {
			url += "&pageNum=" + pageNum;
		}
		if (pageSize != null) {
			url += "&pageSize=" + pageSize;
		}
		CloseableHttpResponse closeableHttpResponse;
		try {
			closeableHttpResponse = HttpClientUtils.get(getHttpClient(), url);
			JSONObject pageInfo = getResult(closeableHttpResponse, JSONObject.class);
			return pageInfo.toJavaObject(new TypeReference<PageInfo<DirAndResource>>() {
			});
		} catch (IOException e) {
			throw new RuntimeException("访问mos服务器失败", e);
		}
	}
	
}
