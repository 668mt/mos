package mt.utils.httpclient;

import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2018/9/24
 */
public class MyHttpClient {
	
	public MyHttpClient(String url) {
		this.url = url;
	}
	
	public MyHttpClient(String url, Method method) {
		this.url = url;
		this.method = method;
	}
	
	public enum Method {
		GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE
	}
	
	@Setter
	private Method method;
	@Setter
	private String url;
	@Setter
	private HttpHost proxyHost;
	//	@Setter
//	private String param;
	private Map<String, String> params;
	@Setter
	private int retry = 1;
	@Setter
	private OnError onError;
	@Setter
	private CallDownload callDownload;
	@Setter
	private OnCheck onCheck;
	@Setter
	private String encoding = "utf-8";
	@Setter
	private Integer socketTimeout;
	@Setter
	private Integer connectTimeout;
	@Setter
	private String contentType = "text/html;charset=utf-8";
	@Setter
	private String accept = "*/*";
	@Setter
	private String acceptLanguage = "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3";
	@Setter
	private String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:52.0) Gecko/20100101 Firefox/52.0";
	@Setter
	private String cookie;
	@Setter
	private Map<String, String> headers;
	@Setter
	private HttpEntity requestEntity;
	@Getter
	private HttpEntity responseEntity;
	@Setter
	private boolean throwException = false;
	@Setter
	private MultipartEntityBuilder multipartEntityBuilder;
	
	public void addHeader(String name, String value) {
		if (headers == null) headers = new HashMap<>();
		headers.put(name, value);
	}
	
	public void setFormSubmit(boolean isFormSubmit) {
		if (isFormSubmit) {
			method = Method.POST;
			contentType = "application/x-www-form-urlencoded";
		}
	}
	
	public void addParam(String key, String value) {
		if (params == null) params = new HashMap<>();
		params.put(key, value);
		setFormSubmit(true);
	}
	
	public void setParam(String param) {
//		this.param = param;
		requestEntity = new StringEntity(param, "utf-8");
	}
	
	private void convertParams(Map<String, String> params) {
		//拼装参数Map为String，含URLEncode
		StringBuilder paramstr = new StringBuilder();
		try {
			for (Map.Entry<String, String> entry : params.entrySet()) {
				paramstr.append(entry.getKey())
						.append("=")
						.append(URLEncoder.encode(entry.getValue(), "UTF-8"))
						.append("&");
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		setParam(paramstr.toString().substring(0, paramstr.length() - 1));
	}
	
	public void addMultipart(String name, ContentBody contentBody) {
		if (multipartEntityBuilder == null)
			multipartEntityBuilder = MultipartEntityBuilder.create();
		multipartEntityBuilder.addPart(name, contentBody);
	}
	
	/**
	 * 绕过验证
	 *
	 * @return
	 */
	public static SSLContext createIgnoreVerifySSL() throws Exception {
		SSLContext sc = SSLContext.getInstance("SSLv3");
		
		// 实现一个X509TrustManager接口，用于绕过验证，不用修改里面的方法
		X509TrustManager trustManager = new X509TrustManager() {
			@Override
			public void checkClientTrusted(
					java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
					String paramString) throws CertificateException {
			}
			
			@Override
			public void checkServerTrusted(
					java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
					String paramString) throws CertificateException {
			}
			
			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};
		
		sc.init(null, new TrustManager[]{trustManager}, null);
		return sc;
	}
	
	public CloseableHttpClient create() {
		//采用绕过验证的方式处理https请求
		SSLContext sslcontext = null;
		try {
			sslcontext = createIgnoreVerifySSL();
			// 设置协议http和https对应的处理socket链接工厂的对象
			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
					.register("http", PlainConnectionSocketFactory.INSTANCE)
					.register("https", new SSLConnectionSocketFactory(sslcontext))
					.build();
			PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
			HttpClientBuilder builder = HttpClients.custom();
			builder.setConnectionManager(connManager);
			if (proxyHost != null) {
				builder.setProxy(proxyHost);
			}
			if (connectTimeout != null || socketTimeout != null) {
				RequestConfig.Builder custom = RequestConfig.custom();
				if (connectTimeout != null) {
					custom.setConnectionRequestTimeout(connectTimeout);
				}
				if (socketTimeout != null) {
					custom.setSocketTimeout(socketTimeout);
				}
				builder.setDefaultRequestConfig(custom.build());
			}
			return builder.build();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String connect() {
		CloseableHttpClient httpClient = create();
		try {
			
			if (multipartEntityBuilder != null) {
				if (params != null) {
					for (Map.Entry<String, String> entry : params.entrySet()) {
						multipartEntityBuilder.addPart(entry.getKey(), new StringBody(entry.getValue(), ContentType.TEXT_PLAIN));
					}
				}
				requestEntity = multipartEntityBuilder.build();
			} else if (params != null) {
				convertParams(params);
			}
			
			if (method == null) method = Method.GET;
			HttpRequestBase request;
			switch (method) {
				case GET:
					request = new HttpGet(url);
					break;
				case POST:
					request = new HttpPost(url);
					break;
				case PUT:
					request = new HttpPut(url);
					break;
				case HEAD:
					request = new HttpHead(url);
					break;
				case PATCH:
					request = new HttpPatch(url);
					break;
				case TRACE:
					request = new HttpTrace(url);
					break;
				case DELETE:
					request = new HttpDelete(url);
					break;
				case OPTIONS:
					request = new HttpOptions(url);
					break;
				default:
					request = new HttpGet(url);
					break;
			}
			request.addHeader("Content-Type", contentType);
			request.addHeader("accept", accept);
			request.addHeader("accept-language", acceptLanguage);
			request.addHeader("user-agent", userAgent);
			request.addHeader("Connection", "Keep-Alive");
			if (cookie != null) request.addHeader("Cookie", cookie);
			if (headers != null) {
				for (Map.Entry<String, String> header : headers.entrySet()) {
					request.addHeader(header.getKey(), header.getValue());
				}
			}
			if (request instanceof HttpEntityEnclosingRequest && requestEntity != null) {
				HttpEntityEnclosingRequest entityEnclosingRequest = (HttpEntityEnclosingRequest) request;
				entityEnclosingRequest.setEntity(requestEntity);
			}
			while (retry >= 1) {
				try {
					CloseableHttpResponse response = httpClient.execute(request);
					int statusCode = response.getStatusLine().getStatusCode();
					if (statusCode >= 200 && statusCode < 300) {
						responseEntity = response.getEntity();
						if (callDownload != null) {
							callDownload.download(this, responseEntity.getContent());
						} else {
							String result = EntityUtils.toString(responseEntity, encoding);
							if (onCheck != null && onCheck.onCheck(this, retry, result)) {
								continue;
							}
							return result;
						}
					}
				} catch (Exception e) {
					if (onError != null) {
						onError.onError(this, e, retry);
					}
					if (retry <= 1) {
						throw new RuntimeException(e);
					}
				} finally {
					retry--;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (throwException) {
				throw new RuntimeException(e);
			}
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
