package mt.utils.httpclient;

public interface OnError {
	public void onError(MyHttpClient myHttpClient, Exception e, int retry);
}
