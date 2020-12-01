package mt.spring.mos.server.utils;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.DefaultUriBuilderFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class HttpClientServletUtils {
	
	public static CloseableHttpResponse httpClientUploadFile(CloseableHttpClient httpClient, String url, InputStream inputStream, String pathname) throws IOException {
		HttpPost httpPost = new HttpPost(url);
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		ContentType contentType = ContentType.create("multipart/form-data", StandardCharsets.UTF_8);
		builder.setContentType(contentType);
		builder.addBinaryBody("file", inputStream, contentType, "file");// 文件流
		builder.addTextBody("pathname", pathname, contentType);// 类似浏览器表单提交，对应input的name和value
		HttpEntity entity = builder.build();
		httpPost.setEntity(entity);
		try {
			return httpClient.execute(httpPost);
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}
	
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
	
	public static CloseableHttpResponse forward(CloseableHttpClient httpclient, String method, String uri, HttpServletRequest request, MultiValueMap<String, String> headers, MultiValueMap<String, String> params, InputStream requestEntity) throws Exception {
		long contentLength = request.getContentLength();
		ContentType contentType = null;
		if (request.getContentType() != null) {
			contentType = ContentType.parse(request.getContentType());
		}
		InputStreamEntity entity = new InputStreamEntity(requestEntity, contentLength, contentType);
		HttpRequest httpRequest = buildHttpRequest(method, uri, entity, headers, params, request);
		HttpHost httpHost = getHttpHost(new URL(uri));
		return forwardRequest(httpclient, httpHost, httpRequest);
	}
	
	public static MultiValueMap<String, String> getRequestHeaders(HttpServletRequest request) {
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String name = headerNames.nextElement();
			if (!isIncludedHeader(name.toLowerCase(), true)) {
				continue;
			}
			Enumeration<String> headerValues = request.getHeaders(name);
			while (headerValues.hasMoreElements()) {
				String value = headerValues.nextElement();
				headers.add(name, value);
			}
		}
		return headers;
	}
	
	public static MultiValueMap<String, String> getQueryParams(HttpServletRequest request) {
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
		if (request.getQueryString() != null) {
			StringTokenizer st = new StringTokenizer(request.getQueryString(), "&");
			int i;
			while (st.hasMoreTokens()) {
				String s = st.nextToken();
				i = s.indexOf("=");
				if (i > 0 && s.length() >= i + 1) {
					String name = s.substring(0, i);
					String value = s.substring(i + 1);
					try {
						name = URLDecoder.decode(name, "UTF-8");
					} catch (Exception ignored) {
					}
					try {
						value = URLDecoder.decode(value, "UTF-8");
					} catch (Exception ignored) {
					}
					queryParams.add(name, value);
				} else if (i == -1) {
					String name = s;
					String value = "";
					try {
						name = URLDecoder.decode(name, "UTF-8");
					} catch (Exception ignored) {
					}
					queryParams.add(name, value);
				}
			}
		}
		return queryParams;
	}
	
	public static CloseableHttpResponse get(CloseableHttpClient httpClient, String url) throws IOException {
		BasicHttpRequest request = new BasicHttpRequest("GET", url);
		return httpClient.execute(getHttpHost(new URL(url)), request);
	}
	
	public static CloseableHttpResponse delete(CloseableHttpClient httpClient, String url) throws IOException {
		BasicHttpRequest request = new BasicHttpRequest("DELETE", url);
		return httpClient.execute(getHttpHost(new URL(url)), request);
	}
	
	public static MultiValueMap<String, String> getFormParams(HttpServletRequest request, MultiValueMap<String, String> queryParams) {
		MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
		Enumeration<String> parameterNames = request.getParameterNames();
		while (parameterNames.hasMoreElements()) {
			String name = parameterNames.nextElement();
			boolean isQueryParam = false;
			for (Map.Entry<String, List<String>> queryParamEntry : queryParams.entrySet()) {
				if (name.equals(queryParamEntry.getKey())) {
					isQueryParam = true;
					break;
				}
			}
			if (isQueryParam) {
				continue;
			}
			String[] parameterValues = request.getParameterValues(name);
			for (String parameterValue : parameterValues) {
				formParams.add(name, parameterValue);
			}
		}
		return formParams;
	}
	
	public static String concatPostBody(MultiValueMap<String, String> formParams, boolean isUrlEncode) throws UnsupportedEncodingException {
		if (formParams == null || formParams.size() == 0) {
			return "";
		}
		StringBuilder body = new StringBuilder();
		for (Map.Entry<String, List<String>> stringListEntry : formParams.entrySet()) {
			List<String> values = stringListEntry.getValue();
			for (String value : values) {
				body.append("&").append(stringListEntry.getKey()).append("=");
				if (isUrlEncode) {
					body.append(URLEncoder.encode(value, "UTF-8"));
				} else {
					body.append(value);
				}
			}
		}
		return body.substring(1);
	}
	
	private static final DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory();
	
	public static void forward(CloseableHttpClient httpclient, String url, HttpServletRequest request, HttpServletResponse response, @Nullable Map<String, String> responseHeaders) throws Exception {
		forward(httpclient, url, request, response, null, responseHeaders);
	}
	
	public static void forward(CloseableHttpClient httpclient, String url, HttpServletRequest request, HttpServletResponse response, @Nullable Map<String, String> requestHeaders, @Nullable Map<String, String> responseHeaders) throws Exception {
		uriFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.URI_COMPONENT);
		URI uri = uriFactory.expand(url);
		forward(httpclient, uri, request, response, requestHeaders, responseHeaders);
	}
	
	public static void forward(CloseableHttpClient httpclient, URI uri, HttpServletRequest request, HttpServletResponse response, @Nullable Map<String, String> requestHeaders, @Nullable Map<String, String> responseHeaders) throws Exception {
		ContentType contentType = null;
		if (request.getContentType() != null) {
			contentType = ContentType.parse(request.getContentType());
		}
		HttpEntity entity = new InputStreamEntity(request.getInputStream(), request.getContentLength(), contentType);
		MultiValueMap<String, String> headers = getRequestHeaders(request);
		MultiValueMap<String, String> queryParams = getQueryParams(request);
		MultiValueMap<String, String> formParams = getFormParams(request, queryParams);
		if (requestHeaders != null) {
			for (Map.Entry<String, String> stringStringEntry : requestHeaders.entrySet()) {
				headers.set(stringStringEntry.getKey().toLowerCase(), stringStringEntry.getValue());
			}
		}
		if (formParams.size() > 0) {
			entity = new StringEntity(concatPostBody(formParams, true), contentType);
		}
		try {
			Collection<Part> parts = request.getParts();
			if (parts.size() > 0) {
				MultipartEntityBuilder builder = MultipartEntityBuilder.create();
				builder.setContentType(contentType);
				for (Part part : parts) {
					builder.addBinaryBody(part.getName(), part.getInputStream(), contentType, part.getSubmittedFileName());
				}
				if (formParams.size() > 0) {
					for (Map.Entry<String, List<String>> stringListEntry : formParams.entrySet()) {
						List<String> values = stringListEntry.getValue();
						for (String value : values) {
							builder.addTextBody(stringListEntry.getKey(), value, contentType);
						}
					}
				}
				entity = builder.build();
			}
		} catch (Exception ignored) {
		}
		HttpRequest httpRequest = buildHttpRequest(request.getMethod().toUpperCase(), uri, entity, headers, queryParams, request);
		HttpHost httpHost = getHttpHost(uri.toURL());
		CloseableHttpResponse closeableHttpResponse = forwardRequest(httpclient, httpHost, httpRequest);
		writeResponse(closeableHttpResponse, request, response, responseHeaders);
	}
	
	public static void writeResponse(CloseableHttpResponse closeableHttpResponse, HttpServletRequest request, HttpServletResponse response, @Nullable Map<String, String> responseHeaders) throws Exception {
		Header[] allHeaders = closeableHttpResponse.getAllHeaders();
		for (Header header : allHeaders) {
			if (!"Content-Encoding".equalsIgnoreCase(header.getName())) {
				response.addHeader(header.getName(), header.getValue());
			}
		}
		response.setStatus(closeableHttpResponse.getStatusLine().getStatusCode());
		if (responseHeaders != null) {
			for (Map.Entry<String, String> stringStringEntry : responseHeaders.entrySet()) {
				response.setHeader(stringStringEntry.getKey(), stringStringEntry.getValue());
			}
		}
		ServletOutputStream outputStream = response.getOutputStream();
		HttpEntity entity = closeableHttpResponse.getEntity();
		if (entity != null) {
			InputStream content = entity.getContent();
			try {
				if (isGzipRequest(request)) {
					content = handleGzipStream(content);
				}
				if (response.getCharacterEncoding() == null) {
					response.setCharacterEncoding("UTF-8");
				}
				writeResponse(content, outputStream);
				outputStream.flush();
			} finally {
				if (content != null) {
					content.close();
				}
				if (outputStream != null) {
					outputStream.close();
				}
			}
		}
	}
	
	public static InputStream handleGzipStream(InputStream in) throws Exception {
		// Record bytes read during GZip initialization to allow to rewind the stream if
		// needed
		//
		RecordingInputStream stream = new RecordingInputStream(in);
		try {
			return new GZIPInputStream(stream);
		} catch (java.util.zip.ZipException | java.io.EOFException ex) {
			if (stream.getBytesRead() == 0) {
				// stream was empty, return the original "empty" stream
				return in;
			} else {
				stream.reset();
				return stream;
			}
		} finally {
			stream.stopRecording();
		}
	}
	
	/**
	 * InputStream recording bytes read to allow for a reset() until recording is stopped.
	 */
	private static class RecordingInputStream extends InputStream {
		
		private InputStream delegate;
		
		private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		RecordingInputStream(InputStream delegate) {
			super();
			this.delegate = Objects.requireNonNull(delegate);
		}
		
		@Override
		public int read() throws IOException {
			int read = delegate.read();
			
			if (buffer != null && read != -1) {
				buffer.write(read);
			}
			
			return read;
		}
		
		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int read = delegate.read(b, off, len);
			
			if (buffer != null && read != -1) {
				buffer.write(b, off, read);
			}
			
			return read;
		}
		
		@Override
		public void reset() {
			if (buffer == null) {
				throw new IllegalStateException("Stream is not recording");
			}
			
			this.delegate = new SequenceInputStream(
					new ByteArrayInputStream(buffer.toByteArray()), delegate);
			this.buffer = new ByteArrayOutputStream();
		}
		
		public int getBytesRead() {
			return (buffer == null) ? -1 : buffer.size();
		}
		
		public void stopRecording() {
			this.buffer = null;
		}
		
		@Override
		public void close() throws IOException {
			this.delegate.close();
		}
		
	}
	
	public static boolean isGzipRequest(HttpServletRequest request) {
		String characterEncoding = request.getHeader("accept-encoding");
		return characterEncoding != null && characterEncoding.contains("gzip");
	}
	
	public static boolean isIncludedHeader(String headerName, boolean addHost) {
		switch (headerName) {
			case "host":
				if (addHost) {
					return true;
				}
			case "connection":
			case "content-length":
			case "server":
			case "transfer-encoding":
			case "x-application-context":
				return false;
			default:
				return true;
		}
	}
	
	public static void writeResponse(InputStream zin, OutputStream out) throws Exception {
		byte[] bytes = new byte[8096];
		int bytesRead;
		while ((bytesRead = zin.read(bytes)) != -1) {
			out.write(bytes, 0, bytesRead);
		}
	}
	
	public static HttpHost getHttpHost(URL host) {
		return new HttpHost(host.getHost(), host.getPort(), host.getProtocol());
	}
	
	public static CloseableHttpResponse forwardRequest(CloseableHttpClient httpclient, HttpHost httpHost, HttpRequest httpRequest) throws IOException {
		return httpclient.execute(httpHost, httpRequest);
	}
	
	private static String buildQueryParam(MultiValueMap<String, String> params) {
		if (params.size() == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, List<String>> stringListEntry : params.entrySet()) {
			List<String> values = stringListEntry.getValue();
			for (String value : values) {
				try {
					sb.append("&").append(stringListEntry.getKey()).append("=").append(URLEncoder.encode(value, "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}
		return sb.substring(1);
	}
	
	public static HttpRequest buildHttpRequest(String verb, String uri, HttpEntity entity, MultiValueMap<String, String> headers, MultiValueMap<String, String> queryParams, HttpServletRequest request) {
		if (queryParams.size() > 0) {
			uri += "?" + buildQueryParam(queryParams);
		}
		try {
			return buildHttpRequest(verb, new URL(uri).toURI(), entity, headers, queryParams, request);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static HttpRequest buildHttpRequest(String verb, URI uri, HttpEntity entity, MultiValueMap<String, String> headers, MultiValueMap<String, String> queryParams, HttpServletRequest request) {
		HttpRequest httpRequest;
		switch (verb.toUpperCase()) {
			case "POST":
				HttpPost httpPost = new HttpPost(uri);
				httpRequest = httpPost;
				httpPost.setEntity(entity);
				break;
			case "PUT":
				HttpPut httpPut = new HttpPut(uri);
				httpRequest = httpPut;
				httpPut.setEntity(entity);
				break;
			case "PATCH":
				HttpPatch httpPatch = new HttpPatch(uri);
				httpRequest = httpPatch;
				httpPatch.setEntity(entity);
				break;
			case "DELETE":
				BasicHttpEntityEnclosingRequest entityRequest = new BasicHttpEntityEnclosingRequest(verb, uri.toString());
				httpRequest = entityRequest;
				entityRequest.setEntity(entity);
				break;
			default:
				httpRequest = new BasicHttpRequest(verb, uri.toString());
		}
		
		httpRequest.setHeaders(convertHeaders(headers));
		return httpRequest;
	}
	
	public static Header[] convertHeaders(MultiValueMap<String, String> headers) {
		List<Header> list = new ArrayList<>();
		for (String name : headers.keySet()) {
			for (String value : headers.get(name)) {
				list.add(new BasicHeader(name, value));
			}
		}
		return list.toArray(new Header[0]);
	}
	
}