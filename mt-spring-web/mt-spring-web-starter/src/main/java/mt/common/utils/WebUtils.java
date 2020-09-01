package mt.common.utils;

import mt.utils.ConvertUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;


/**
 * @author Martin
 * @ClassName: WebUtils
 * @Description:
 * @date 2017-10-13 下午5:48:37
 */
public final class WebUtils {
	
	/**
	 * PoolingHttpClientConnectionManager
	 */
	private static final PoolingHttpClientConnectionManager HTTP_CLIENT_CONNECTION_MANAGER;
	
	/**
	 * CloseableHttpClient
	 */
	private static final CloseableHttpClient HTTP_CLIENT;
	
	static {
		HTTP_CLIENT_CONNECTION_MANAGER = new PoolingHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", SSLConnectionSocketFactory.getSocketFactory()).build());
		HTTP_CLIENT_CONNECTION_MANAGER.setDefaultMaxPerRoute(100);
		HTTP_CLIENT_CONNECTION_MANAGER.setMaxTotal(200);
		RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(60000).setConnectTimeout(60000).setSocketTimeout(60000).build();
		HTTP_CLIENT = HttpClientBuilder.create().setConnectionManager(HTTP_CLIENT_CONNECTION_MANAGER).setDefaultRequestConfig(requestConfig).build();
	}
	
	/**
	 * 不可实例化
	 */
	private WebUtils() {
	}
	
	/**
	 * 获取HttpServletRequest
	 *
	 * @return HttpServletRequest
	 */
	public static HttpServletRequest getRequest() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		return requestAttributes != null && requestAttributes instanceof ServletRequestAttributes ? ((ServletRequestAttributes) requestAttributes).getRequest() : null;
	}
	
	/**
	 * 获取HttpServletResponse
	 *
	 * @return HttpServletResponse
	 */
	public static HttpServletResponse getResponse() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		return requestAttributes != null && requestAttributes instanceof ServletRequestAttributes ? ((ServletRequestAttributes) requestAttributes).getResponse() : null;
	}
	
	/**
	 * 判断是否为AJAX请求
	 *
	 * @param request HttpServletRequest
	 * @return 是否为AJAX请求
	 */
	public static boolean isAjaxRequest(HttpServletRequest request) {
		assert request != null;
		
		return StringUtils.equalsIgnoreCase(request.getHeader("X-Requested-With"), "XMLHttpRequest");
	}
	
	/**
	 * 添加cookie
	 *
	 * @param request  HttpServletRequest
	 * @param response HttpServletResponse
	 * @param name     Cookie名称
	 * @param value    Cookie值
	 * @param maxAge   有效期(单位: 秒)
	 * @param path     路径
	 * @param domain   域
	 * @param secure   是否启用加密
	 */
	@SuppressWarnings("deprecation")
	public static void addCookie(HttpServletRequest request, HttpServletResponse response, String name, String value, Integer maxAge, String path, String domain, Boolean secure) {
		assert request != null;
		assert response != null;
		Assert.hasText(name);
		Assert.hasText(value);
		
		try {
			name = URLEncoder.encode(name, "UTF-8");
			value = URLEncoder.encode(value, "UTF-8");
			Cookie cookie = new Cookie(name, value);
			if (maxAge != null) {
				cookie.setMaxAge(maxAge);
			}
			if (StringUtils.isNotEmpty(path)) {
				cookie.setPath(path);
			}
			if (StringUtils.isNotEmpty(domain)) {
				cookie.setDomain(domain);
			}
			if (secure != null) {
				cookie.setSecure(secure);
			}
			response.addCookie(cookie);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	/**
	 * 添加cookie
	 *
	 * @param request  HttpServletRequest
	 * @param response HttpServletResponse
	 * @param name     Cookie名称
	 * @param value    Cookie值
	 * @param maxAge   有效期(单位: 秒)
	 */
	@SuppressWarnings("deprecation")
	public static void addCookie(HttpServletRequest request, HttpServletResponse response, String name, String value, Integer maxAge) {
		assert request != null;
		assert response != null;
		Assert.hasText(name);
		Assert.hasText(value);
		
		addCookie(request, response, name, value, maxAge, "/", null, null);
	}
	
	/**
	 * 添加cookie
	 *
	 * @param request  HttpServletRequest
	 * @param response HttpServletResponse
	 * @param name     Cookie名称
	 * @param value    Cookie值
	 */
	public static void addCookie(HttpServletRequest request, HttpServletResponse response, String name, String value) {
		assert request != null;
		assert response != null;
		Assert.hasText(name);
		Assert.hasText(value);
		
		addCookie(request, response, name, value, null, "/", null, null);
	}
	
	/**
	 * 获取cookie
	 *
	 * @param request HttpServletRequest
	 * @param name    Cookie名称
	 * @return Cookie值，若不存在则返回null
	 */
	public static String getCookie(HttpServletRequest request, String name) {
		assert request != null;
		Assert.hasText(name);
		
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			try {
				name = URLEncoder.encode(name, "UTF-8");
				for (Cookie cookie : cookies) {
					if (name.equals(cookie.getName())) {
						return URLDecoder.decode(cookie.getValue(), "UTF-8");
					}
				}
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		return null;
	}
	
	/**
	 * 移除cookie
	 *
	 * @param request  HttpServletRequest
	 * @param response HttpServletResponse
	 * @param name     Cookie名称
	 * @param path     路径
	 * @param domain   域
	 */
	public static void removeCookie(HttpServletRequest request, HttpServletResponse response, String name, String path, String domain) {
		assert request != null;
		assert response != null;
		Assert.hasText(name);
		
		try {
			name = URLEncoder.encode(name, "UTF-8");
			Cookie cookie = new Cookie(name, null);
			cookie.setMaxAge(0);
			if (StringUtils.isNotEmpty(path)) {
				cookie.setPath(path);
			}
			if (StringUtils.isNotEmpty(domain)) {
				cookie.setDomain(domain);
			}
			response.addCookie(cookie);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	/**
	 * 移除cookie
	 *
	 * @param request  HttpServletRequest
	 * @param response HttpServletResponse
	 * @param name     Cookie名称
	 */
	public static void removeCookie(HttpServletRequest request, HttpServletResponse response, String name) {
		assert request != null;
		assert response != null;
		Assert.hasText(name);
		
		removeCookie(request, response, name, "/", null);
	}
	
	/**
	 * 参数解析
	 *
	 * @param query    查询字符串
	 * @param encoding 编码格式
	 * @return 参数
	 */
	public static Map<String, String> parse(String query, String encoding) {
		Assert.hasText(query);
		
		Charset charset;
		if (StringUtils.isNotEmpty(encoding)) {
			charset = Charset.forName(encoding);
		} else {
			charset = Charset.forName("UTF-8");
		}
		List<NameValuePair> nameValuePairs = URLEncodedUtils.parse(query, charset);
		Map<String, String> parameterMap = new HashMap<String, String>();
		for (NameValuePair nameValuePair : nameValuePairs) {
			parameterMap.put(nameValuePair.getName(), nameValuePair.getValue());
		}
		return parameterMap;
	}
	
	/**
	 * 解析参数
	 *
	 * @param query 查询字符串
	 * @return 参数
	 */
	public static Map<String, String> parse(String query) {
		Assert.hasText(query);
		
		return parse(query, null);
	}
	
	/**
	 * 重定向
	 *
	 * @param request          HttpServletRequest
	 * @param response         HttpServletResponse
	 * @param url              URL
	 * @param contextRelative  是否相对上下文路径
	 * @param http10Compatible 是否兼容HTTP1.0
	 */
	public static void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url, boolean contextRelative, boolean http10Compatible) {
		assert request != null;
		assert response != null;
		Assert.hasText(url);
		
		StringBuilder targetUrl = new StringBuilder();
		if (contextRelative && url.startsWith("/")) {
			targetUrl.append(request.getContextPath());
		}
		targetUrl.append(url);
		String encodedRedirectURL = response.encodeRedirectURL(targetUrl.toString());
		if (http10Compatible) {
			try {
				response.sendRedirect(encodedRedirectURL);
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		} else {
			response.setStatus(303);
			response.setHeader("Location", encodedRedirectURL);
		}
	}
	
	/**
	 * 重定向
	 *
	 * @param request  HttpServletRequest
	 * @param response HttpServletResponse
	 * @param url      URL
	 */
	public static void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url) {
		sendRedirect(request, response, url, true, true);
	}
	
	/**
	 * POST请求
	 *
	 * @param url          URL
	 * @param parameterMap 请求参数
	 * @return 返回结果
	 */
	public static String post(String url, Map<String, Object> parameterMap) {
		Assert.hasText(url);
		
		String result = null;
		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			if (parameterMap != null) {
				for (Entry<String, Object> entry : parameterMap.entrySet()) {
					String name = entry.getKey();
//					String value = ConvertUtils.convert(entry.getValue());
					String value = ConvertUtils.convert(entry.getValue(),String.class);
					if (StringUtils.isNotEmpty(name)) {
						nameValuePairs.add(new BasicNameValuePair(name, value));
					}
				}
			}
			HttpPost httpPost = new HttpPost(url);
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
			CloseableHttpResponse httpResponse = HTTP_CLIENT.execute(httpPost);
			try {
				HttpEntity httpEntity = httpResponse.getEntity();
				if (httpEntity != null) {
					result = EntityUtils.toString(httpEntity);
					EntityUtils.consume(httpEntity);
				}
			} finally {
				try {
					httpResponse.close();
				} catch (IOException e) {
				}
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (ClientProtocolException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (ParseException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return result;
	}
	
	/**
	 * GET请求
	 *
	 * @param url          URL
	 * @param parameterMap 请求参数
	 * @return 返回结果
	 */
	public static String get(String url, Map<String, Object> parameterMap) {
		Assert.hasText(url);
		
		String result = null;
		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			if (parameterMap != null) {
				for (Entry<String, Object> entry : parameterMap.entrySet()) {
					String name = entry.getKey();
//					String value = ConvertUtils.convert(entry.getValue());
					String value = ConvertUtils.convert(entry.getValue(),String.class);
					if (StringUtils.isNotEmpty(name)) {
						nameValuePairs.add(new BasicNameValuePair(name, value));
					}
				}
			}
			HttpGet httpGet = new HttpGet(url + (StringUtils.contains(url, "?") ? "&" : "?") + EntityUtils.toString(new UrlEncodedFormEntity(nameValuePairs, "UTF-8")));
			CloseableHttpResponse httpResponse = HTTP_CLIENT.execute(httpGet);
			try {
				HttpEntity httpEntity = httpResponse.getEntity();
				if (httpEntity != null) {
					result = EntityUtils.toString(httpEntity);
					EntityUtils.consume(httpEntity);
				}
			} finally {
				try {
					httpResponse.close();
				} catch (IOException e) {
				}
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (ParseException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (ClientProtocolException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return result;
	}
	
	/**
	 * POST请求
	 *
	 * @param url URL
	 * @param xml XML
	 * @return 返回结果
	 */
	public static String post(String url, String xml) {
		Assert.hasText(url);
		
		String result = null;
		try {
			HttpPost httpPost = new HttpPost(url);
			httpPost.setEntity(new StringEntity(xml, "UTF-8"));
			CloseableHttpResponse httpResponse = HTTP_CLIENT.execute(httpPost);
			try {
				HttpEntity httpEntity = httpResponse.getEntity();
				if (httpEntity != null) {
					result = EntityUtils.toString(httpEntity, "UTF-8");
					EntityUtils.consume(httpEntity);
				}
			} finally {
				try {
					httpResponse.close();
				} catch (IOException e) {
				}
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (ClientProtocolException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (ParseException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return result;
	}
	
	/**
	 * 获取request body内容
	 *
	 * @param request
	 * @return
	 */
	public static String getContent(HttpServletRequest request) {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
			StringBuilder sb = new StringBuilder();
			String buffer = null;
			while ((buffer = br.readLine()) != null) {
				sb.append(buffer + "\r\n");
			}
			br.close();
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 将Map的键值对用符号连接起来
	 *
	 * @param params
	 * @param sign
	 * @param URLEncode
	 * @return
	 */
	public static String concat(Map<String, String> params, String sign, boolean URLEncode) {
		assert params != null;
		
		sign = sign == null ? "&" : sign;
		Set<Entry<String, String>> entrySet = params.entrySet();
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> entry : entrySet) {
			if (URLEncode) {
				try {
					sb.append(URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + URLEncoder.encode(entry.getValue(), "UTF-8") + sign);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			} else {
				sb.append(entry.getKey() + "=" + entry.getValue() + sign);
			}
		}
		String str = sb.toString();
		int lastIndexOf = str.lastIndexOf(sign);
		if (lastIndexOf != -1) {
			return str.substring(0, lastIndexOf);
		} else {
			return "";
		}
	}
	
	/**
	 * 将Map的键值对用符号连接起来
	 *
	 * @param params
	 * @param sign
	 * @return
	 */
	public static String concat(Map<String, String> params, String sign) {
		return concat(params, sign, true);
	}
	
	/**
	 * 将Map的键值对用符号连接起来
	 *
	 * @param params
	 * @return
	 */
	public static String concat(Map<String, String> params) {
		return concat(params, "&", true);
	}
	
	/**
	 * 连接网址和参数
	 *
	 * @param url
	 * @param param
	 * @return
	 */
	public static String concat(String url, String param) {
		assert url != null;
		param = param == null ? "" : param;
		return url.contains("?") ? (url.endsWith("&") ? url + param : url + "&" + param) : url + "?" + param;
	}
}