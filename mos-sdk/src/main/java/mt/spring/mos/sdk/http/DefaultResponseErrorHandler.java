package mt.spring.mos.sdk.http;

import org.apache.http.client.methods.CloseableHttpResponse;

/**
 * @Author Martin
 * @Date 2021/1/24
 */
public class DefaultResponseErrorHandler implements ResponseErrorHandler {
	
	@Override
	public boolean hasError(CloseableHttpResponse response) {
		int statusCode = response.getStatusLine().getStatusCode();
		return statusCode < 200 || statusCode >= 300;
	}
	
	@Override
	public void handError(CloseableHttpResponse response) {
		throw new IllegalStateException("请求错误:" + response.getStatusLine().getStatusCode());
	}
}
