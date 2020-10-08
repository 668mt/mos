package mt.spring.mos.sdk;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.util.MultiValueMap;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

public class HttpClientUtils {
	
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
	
	
	public static CloseableHttpResponse get(CloseableHttpClient httpClient, String url) throws IOException {
		BasicHttpRequest request = new BasicHttpRequest("GET", url);
		return httpClient.execute(getHttpHost(new URL(url)), request);
	}
	
	public static CloseableHttpResponse delete(CloseableHttpClient httpClient, String url) throws IOException {
		BasicHttpRequest request = new BasicHttpRequest("DELETE", url);
		return httpClient.execute(getHttpHost(new URL(url)), request);
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
	
	public static InputStream handleGzipStream(InputStream in) throws Exception {
		// Record bytes read during GZip initialization to allow to rewind the stream if
		// needed
		//
		RecordingInputStream stream = new RecordingInputStream(in);
		try {
			return new GZIPInputStream(stream);
		} catch (java.util.zip.ZipException | EOFException ex) {
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