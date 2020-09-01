package mt.utils.http;

public interface OnError {
	public void onError(Exception e, int retry);
}
