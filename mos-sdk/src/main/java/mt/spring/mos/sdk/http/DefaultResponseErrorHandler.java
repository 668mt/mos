package mt.spring.mos.sdk.http;

import okhttp3.Response;

/**
 * @Author Martin
 * @Date 2021/1/24
 */
public class DefaultResponseErrorHandler implements ResponseErrorHandler {
	
	@Override
	public boolean hasError(Response response) {
		return !response.isSuccessful();
	}
	
	@Override
	public void handError(Response response) {
		throw new IllegalStateException("请求错误:" + response.code());
	}
}
