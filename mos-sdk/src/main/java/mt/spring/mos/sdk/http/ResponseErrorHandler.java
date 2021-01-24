package mt.spring.mos.sdk.http;

import org.apache.http.client.methods.CloseableHttpResponse;

public interface ResponseErrorHandler {
	boolean hasError(CloseableHttpResponse response);
	
	void handError(CloseableHttpResponse response);
}