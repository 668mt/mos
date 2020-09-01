package mt.utils.http;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * 网络连接工具
* @ClassName: MyHttp
* @Description:  version 1.0.3
* @author Martin
* @date 2017-10-12 上午11:04:02
*
 */
public class MyHttp {
//	interface CallDownload{
//		public void download(InputStream is);
//	}
//	interface OnError {
//		public void onError(Throwable e,int retry);
//	} 
	private OnError onError;
	private CallDownload callDownload;
	@Getter
	@Setter
	private OnCheck onCheck;

	private String url;
	private String encode = "utf-8";
	private int retry = 1;
	private Object param;
	private String method = "get";
	private boolean isFormSubmit = false;
	private String lineSepreat = "\r\n";
	private int readTimeout = 30000;
	private int connectTimeout = 30000;
	private int responseCode;
	private HttpURLConnection httpURLConnection;
	
	private String contentType = "text/html;charset=utf-8";
	private String accept = "*/*";
	private String acceptLanguage = "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3";
	private String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:52.0) Gecko/20100101 Firefox/52.0";
	private String cookie;
	private Map<String, String> headers = new HashMap<String, String>();
	@Getter
	private DaiLi daiLi;

	public void setDaiLi(DaiLi daiLi){
		this.daiLi = daiLi;
	}


	public void addHeader(String key, String value){
		headers.put(key, value);
	}
	public void setParams(Map<String, String> params){
		setFormSubmit(true);
		//拼装参数Map为String，含URLEncode
		StringBuffer paramstr = new StringBuffer();
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
		param = paramstr.toString().substring(0,paramstr.length()-1);
	}
	public void setParams2(Map<String, Object> params){
		setFormSubmit(true);
		//拼装参数Map为String，含URLEncode
		StringBuffer paramstr = new StringBuffer();
		try {
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				paramstr.append(entry.getKey())
				.append("=")
				.append(URLEncoder.encode(entry.getValue()+"", "UTF-8"))
				.append("&");
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		param = paramstr.toString().substring(0,paramstr.length()-1);
	}
	/**
	 * 连接地址
	 * @return 网页结果
	 */
	public String connect() {
		//是否是表单提交
		if(isFormSubmit){
			contentType = "application/x-www-form-urlencoded";
		}
		
    	PrintWriter out = null;
    	BufferedReader in = null;
    	StringBuffer sb = new StringBuffer();
		//是否使用代理
		SocketAddress addr = null;
		Proxy proxy = null;
		long start = System.currentTimeMillis();
		if(getDaiLi() != null && getDaiLi().getIp() != null && getDaiLi().getPort() != null){
			addr = new InetSocketAddress(daiLi.getIp(), daiLi.getPort());
			proxy = new Proxy(Proxy.Type.HTTP, addr);
		}
    	try {
    		URL realUrl = new URL(url);
    		// 打开和URL之间的连接
    		HttpURLConnection conn = null;
			if(proxy != null){
				conn = (HttpURLConnection) realUrl.openConnection(proxy);
			}else{
				conn = (HttpURLConnection) realUrl.openConnection();
			}
    		
    		conn.setDoOutput(true);
    		conn.setDoInput(true);
    		conn.setUseCaches(false);
    		
    		conn.setReadTimeout(readTimeout);
    		conn.setConnectTimeout(connectTimeout);
    		
    		conn.setRequestMethod(method.toUpperCase());
    		conn.setRequestProperty("Content-Type", contentType);
    		conn.setRequestProperty("accept", accept);
    		conn.setRequestProperty("accept-language", acceptLanguage);
    		conn.setRequestProperty("user-agent",userAgent);
    		conn.setRequestProperty("Connection", "Keep-Alive");

    		if(cookie!=null){
    			conn.setRequestProperty("Cookie", cookie);
    		}
    		//新增header
    		Set<String> keySet = headers.keySet();
    		if(keySet!=null && keySet.size()>0){
	    		for (String key : keySet) {
	    			conn.setRequestProperty(key, headers.get(key));
				}
    		}
    		
    		httpURLConnection = conn;
    		
    		//写入数据
    		if(param!=null){
	    		out = new PrintWriter(conn.getOutputStream());
	    		out.print(param);
	    		out.flush();
    		}
    		// 定义BufferedReader输入流来读取URL的响应
    		InputStream is = conn.getInputStream();
    		responseCode = conn.getResponseCode();
    		
    		//解析响应头
    		parseHeaderFields(conn.getHeaderFields());
    		
    		//进行文件下载
    		if(callDownload!=null){
    			callDownload.download(is);
    			return null;
    		}
    		in = new BufferedReader(
    				new InputStreamReader(is,encode));
    		String line;
    		while ((line = in.readLine()) != null) {
    			sb.append(line+lineSepreat);
    		}
    		long end = System.currentTimeMillis();
    		//耗费时间
    		if(daiLi != null){
    			daiLi.addSuccess();
    			daiLi.setConnectTime((end - start) * 1.0 / 1000);
			}
    	} catch (Exception e) {
    		if(daiLi != null){
    			daiLi.addError();
			}
    		//连接失败，进行重连
    		retry--;
    		//回调函数不为空的时候，进行回调
    		if(onError!=null){
    			onError.onError(e,retry);
    		}
    		if(retry>0){
    			return connect();
    		}
    		if(retry <= 0 && onError == null){
    			throw new RuntimeException(e);
    		}
    	}finally{
    		try{
    			if(out!=null){
    				out.close();
    			}
    			if(in!=null){
    				in.close();
    			}
    		}catch(IOException ex){
//    			ex.printStackTrace();
    		}
    	}
    	if(onCheck != null){
			boolean flag = onCheck.onCheck(retry,sb.toString(),responseHeaders);
			if(!flag){
				return connect();
			}
		}
    	return sb.toString();
    }

	private Map<String, List<String>> responseHeaders;
	private String responseCookie = "";
	
	/**
	 * 解析响应头
	 * @param headerFields
	 */
	private void parseHeaderFields(Map<String, List<String>> headerFields) {
		responseHeaders = headerFields;
        Set<String> set= headerFields.keySet();
        for (Iterator<String> iterator = set.iterator(); iterator.hasNext();) {
            String key = (String) iterator.next();
            if(key!=null){
                if (key.equals("Set-Cookie")) {
                    List<String> list = (List<String>) headerFields.get(key);
                    for (String str : list) {
                        String temp=str.split(";")[0];
                        responseCookie+=temp+";";
                    }
                }
            }
        } 
	}
	
	public OnError getOnError() {
		return onError;
	}
	public void setOnError(OnError onError) {
		this.onError = onError;
	}
	public CallDownload getCallDownload() {
		return callDownload;
	}
	public MyHttp setCallDownload(CallDownload callDownload) {
		this.callDownload = callDownload;
		return this;
	}
	public int getReadTimeout() {
		return readTimeout;
	}
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}
	public int getConnectTimeout() {
		return connectTimeout;
	}
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}
	
	public int getResponseCode() {
		return responseCode;
	}
	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}
	public HttpURLConnection getHttpURLConnection() {
		return httpURLConnection;
	}
	public void setHttpURLConnection(HttpURLConnection httpURLConnection) {
		this.httpURLConnection = httpURLConnection;
	}
	public Map<String, List<String>> getResponseHeaders() {
		return responseHeaders;
	}
	public void setResponseHeaders(Map<String, List<String>> responseHeaders) {
		this.responseHeaders = responseHeaders;
	}
	public String getResponseCookie() {
		return responseCookie;
	}
	public void setResponseCookie(String responseCookie) {
		this.responseCookie = responseCookie;
	}
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public boolean isFormSubmit() {
		return isFormSubmit;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
	public void setFormSubmit(boolean isFormSubmit) {
		this.isFormSubmit = isFormSubmit;
	}

	public String getEncode() {
		return encode;
	}

	public void setEncode(String encode) {
		this.encode = encode;
	}

	public int getRetry() {
		return retry;
	}

	public void setRetry(int retry) {
		this.retry = retry;
	}

	public Object getParam() {
		return param;
	}

	public void setParam(Object param) {
		this.param = param;
	}

	public String getLineSepreat() {
		return lineSepreat;
	}

	public void setLineSepreat(String lineSepreat) {
		this.lineSepreat = lineSepreat;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getAccept() {
		return accept;
	}

	public void setAccept(String accept) {
		this.accept = accept;
	}

	public String getAcceptLanguage() {
		return acceptLanguage;
	}

	public void setAcceptLanguage(String acceptLanguage) {
		this.acceptLanguage = acceptLanguage;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public void setCookie(String cookie) {
		this.cookie = cookie;
	}

	public Map<String, String> getHeaders() {
		headers.put("Content-Type", contentType);
		if(cookie!=null){
			headers.put("Cookie", cookie);
		}
		headers.put("Accept", accept);
		headers.put("Accept-language", acceptLanguage);
		headers.put("User-Agent", userAgent);
		return headers;
	}

	public MyHttp(String url, String param, String method) {
		super();
		this.url = url;
		this.param = param;
		this.method = method;
	}

	public MyHttp(String url, String method) {
		super();
		this.url = url;
		this.method = method;
	}

	public MyHttp(String url, String method, int retry) {
		super();
		this.url = url;
		this.retry = retry;
		this.method = method;
	}

	public MyHttp(String url, String param, String method, int retry,
				  boolean isFormSubmit) {
		super();
		this.url = url;
		this.retry = retry;
		this.param = param;
		this.method = method;
		this.isFormSubmit = isFormSubmit;
	}
	public MyHttp(String url) {
		super();
		this.url = url;
	}
	public MyHttp(String url, int retry) {
		super();
		this.url = url;
		this.retry = retry;
	}
	
	/**
	 * 下载为字节
	 * @param url
	 * @return
	 */
	public static byte[] toByteArray(String url) throws Exception {
		final Map<String, byte[]> map = new HashMap<>();
		MyHttp myHttp = new MyHttp(url);
		myHttp.setCallDownload(new CallDownload() {
			@Override
			public void download(InputStream is) throws IOException {
				map.put("data", IOUtils.toByteArray(is));
			}
		}).connect();
		return map.get("data");
	}
	
}
