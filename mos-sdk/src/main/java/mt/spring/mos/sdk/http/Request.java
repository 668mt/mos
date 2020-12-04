package mt.spring.mos.sdk.http;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.SneakyThrows;
import mt.spring.mos.sdk.utils.Assert;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.message.BasicHttpRequest;
import org.springframework.util.MultiValueMap;

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
	
	@SneakyThrows
	public HttpRequest buildRequest() {
		HttpRequest httpRequest = null;
		switch (method) {
			case GET:
			case DELETE:
				httpRequest = new BasicHttpRequest(method.name(), url);
				break;
			case POST:
			case PUT:
				switch (contentType) {
					case APPLICATION_JSON:
						HttpEntityEnclosingRequestBase httpEntityEnclosingRequestBase = method == HttpMethod.POST ? new HttpPost() : new HttpPut();
						StringEntity stringEntity = new StringEntity(JSONObject.toJSONString(body), contentType.getCharset());
						httpEntityEnclosingRequestBase.setEntity(stringEntity);
						httpRequest = httpEntityEnclosingRequestBase;
						break;
					case APPLICATION_FORM_URLENCODED:
					case APPLICATION_FORM_DATA:
						MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
						org.apache.http.entity.ContentType parsedContentType = org.apache.http.entity.ContentType.parse(contentType.getValue());
						multipartEntityBuilder.setContentType(parsedContentType);
						for (Map.Entry<String, Object> stringListEntry : body.entrySet()) {
							String name = stringListEntry.getKey();
							Object value = stringListEntry.getValue();
							if (value instanceof ContentBody) {
								ContentBody contentBody = (ContentBody) value;
								multipartEntityBuilder.addPart(name, contentBody);
							} else {
								multipartEntityBuilder.addTextBody(name, value.toString(), parsedContentType);
							}
						}
						break;
				}
				break;
		}
		Assert.notNull(httpRequest, "httpRequest构建失败");
		if (headers != null) {
			for (Map.Entry<String, List<Object>> stringListEntry : headers.entrySet()) {
				for (Object o : stringListEntry.getValue()) {
					httpRequest.addHeader(stringListEntry.getKey(), o.toString());
				}
			}
		}
		return httpRequest;
	}
	
}
