package mt.spring.mos.sdk;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.cloud.commons.httpclient.DefaultApacheHttpClientConnectionManagerFactory;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2020/5/20
 */
@Data
@Slf4j
public class OssSdk {
	private String host;
	private String publicKey;
	private String bucketName;
	private Long openId;
	private CloseableHttpClient httpClient;
	
	public HttpClientConnectionManager newConnectionManager() {
		DefaultApacheHttpClientConnectionManagerFactory connectionManagerFactory = new DefaultApacheHttpClientConnectionManagerFactory();
		HttpClientConnectionManager connectionManager = connectionManagerFactory.newConnectionManager(
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
	
	public OssSdk(String host, Long openId, String bucketName, String publicKey) {
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
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(host)
					.append("/oss/")
					.append(bucketName)
					.append(pathname)
					.append("?sign=")
					.append(sign)
					.append("&openId=")
					.append(openId);
			return stringBuilder.toString();
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
			log.info("oss开始上传：{}", pathname);
			String sign = getSign(pathname, 3600L);
			CloseableHttpResponse response = HttpClientUtils.httpClientUploadFile(getHttpClient(), host + "/upload/" + bucketName + "?openId=" + openId + "&sign=" + URLEncoder.encode(sign, "UTF-8"), inputStream, pathname);
			String s = EntityUtils.toString(response.getEntity());
			log.info("oss上传结果：{}", s);
			JSONObject jsonObject = JSONObject.parseObject(s);
			Assert.state(jsonObject.getString("status").equalsIgnoreCase("ok"), "上传失败:" + jsonObject.getString("message"));
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}
	
	private String getSignQueryParams(String pathname, Long expireSeconds) throws UnsupportedEncodingException {
		String sign = getSign(pathname, 30L);
		pathname = URLEncoder.encode(pathname, "UTF-8");
		sign = URLEncoder.encode(sign, "UTF-8");
		return "sign=" + sign + "&openId=" + openId + "&pathname=" + pathname + "&bucketName=" + bucketName;
	}
	
	private <T> T getResult(CloseableHttpResponse response, Class<T> type) throws IOException {
		HttpEntity entity = response.getEntity();
		String s = EntityUtils.toString(entity, "UTF-8");
		log.debug("请求结果：{}", s);
		JSONObject jsonObject = JSONObject.parseObject(s);
		return jsonObject.getObject("result", type);
	}
	
	/**
	 * 判断文件是否存在
	 *
	 * @param pathname
	 * @return
	 * @throws IOException
	 */
	public boolean isExists(@NotNull String pathname) throws IOException {
		CloseableHttpResponse closeableHttpResponse = HttpClientUtils.get(getHttpClient(), host + "/upload/" + bucketName + "/isExists?" + getSignQueryParams(pathname, 30L));
		return getResult(closeableHttpResponse, Boolean.class);
	}
	
	/**
	 * 删除文件
	 *
	 * @param pathname
	 * @return
	 * @throws IOException
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
	
	public static void main(String[] args) throws IOException {
		LoggingSystem loggingSystem = LoggingSystem.get(OssSdk.class.getClassLoader());
		loggingSystem.setLogLevel("root", LogLevel.INFO);
		String ak = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCugDg_ZE5gEwVXMMS-EDIe5g_dX-YPKKER5nJeedvAOwTppWksjebyD6D00KnQ62GwK5974pPmv0Gp_pLwI5uFthOQ3Zft_jM4gDXwzNJ2MqJ-7WM1b_pI6zeinXXx1T3jjT4AEoCHQy7C1ZVZbzwpTpQJc0rclelZ4kaNP1wz2QIDAQAB";
		OssSdk ossSdk = new OssSdk("http://localhost:9700", 1L, "defaultBucket", ak);
		System.out.println(ossSdk.getUrl("/mc/image/" + UUID.randomUUID().toString() + ".jpg", 3600L));
//		ossSdk.deleteFile("conf/test.mp4");
//		ossSdk.upload("conf/test.mp4", new FileInputStream("H:\\movies\\庆余年\\庆余年-1.mp4"), new UploadProcessListener() {
//			@Override
//			public void update(double percent) {
//				System.out.println("上传进度：" + percent);
//			}
//		});
//		System.out.println(ossSdk.getSign("file", 3600L));
//		System.out.println(ossSdk.isExists("/剑王朝/剑王朝-1.mp4"));
//		System.out.println(ossSdk.getUrl("/剑王朝/剑王朝-1.mp4", 1800L, false));
//		//获取资源
//		System.out.println(ossSdk.getUrl("ReadMe2.txt", 3600L));
//		//上传文件
//		ossSdk.upload("/nginx/mc.conf", 30L, new FileInputStream("G:\\softwares\\nginx-1.13.6\\server\\mc.conf"));
	}
	
}
