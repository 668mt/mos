package mt.spring.mos.sdk.http;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.sdk.utils.MultiValueMap;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2020/11/23
 */
@Data
public class Request {
	private String url;
	private HttpMethod method;
	private ContentType contentType;
	private MultiValueMap<String, Object> headers;
	private Map<String, Object> body;
	
	public enum HttpMethod {
		GET, POST, DELETE, PUT
	}
	
	public enum ContentType {
		APPLICATION_FORM_URLENCODED("application/x-www-form-urlencoded"),
		APPLICATION_JSON("application/json"),
		APPLICATION_FORM_DATA("multipart/form-data");
		private final String value;
		private final String charset;
		
		ContentType(String value) {
			this(value, "utf-8");
		}
		
		ContentType(String value, String charset) {
			this.value = value;
			this.charset = charset;
		}
		
		public String getValue() {
			return value;
		}
		
		public String getCharset() {
			return charset;
		}
	}
	
	/**
	 * 构建 okhttp 请求
	 *
	 * @return
	 */
	public okhttp3.Request buildRequest() {
		Assert.notNull(url, "url can not be null");
		Assert.notNull(method, "method can not be null");
		okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(url);
		switch (method) {
			case GET:
				builder.get();
				break;
			case DELETE:
				builder.delete();
				break;
			case POST:
				builder.post(buildBody());
				break;
			case PUT:
				builder.put(buildBody());
				break;
		}
		if (headers != null) {
			for (Map.Entry<String, List<Object>> entry : headers.entrySet()) {
				for (Object value : entry.getValue()) {
					builder.addHeader(entry.getKey(), value.toString());
				}
			}
		}
		return builder.build();
	}
	
	/**
	 * 构建请求体
	 *
	 * @return
	 */
	private RequestBody buildBody() {
		if (contentType == null || body == null) {
			return RequestBody.create(new byte[0], null);
		}
		switch (contentType) {
			case APPLICATION_JSON: {
				MediaType mediaType = MediaType.parse(contentType.getValue() + "; charset=" + contentType.getCharset());
				return RequestBody.create(JSONObject.toJSONString(body), mediaType);
			}
			case APPLICATION_FORM_URLENCODED:
			case APPLICATION_FORM_DATA: {
				MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
				for (Map.Entry<String, Object> entry : body.entrySet()) {
					String name = entry.getKey();
					Object value = entry.getValue();
					if (value instanceof RequestBody) {
						multipartBuilder.addFormDataPart(name, null, (RequestBody) value);
					} else {
						multipartBuilder.addFormDataPart(name, value.toString());
					}
				}
				return multipartBuilder.build();
			}
			default:
				return RequestBody.create(new byte[0], null);
		}
	}
	
}
