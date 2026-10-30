package mt.spring.mos.sdk.http;

import okhttp3.Response;

/**
 * @Author Martin
 * @Date 2021/1/24
 */
public interface ResponseErrorHandler {
	boolean hasError(Response response);
	
	void handError(Response response);
}
