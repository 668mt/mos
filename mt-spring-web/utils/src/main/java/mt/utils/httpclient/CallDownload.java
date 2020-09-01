package mt.utils.httpclient;

import java.io.InputStream;

public interface CallDownload {
	public void download(MyHttpClient myHttpClient, InputStream is) throws Exception;
}
