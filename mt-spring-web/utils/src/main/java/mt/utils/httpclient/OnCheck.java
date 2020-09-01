package mt.utils.httpclient;

import java.util.List;
import java.util.Map;

public interface OnCheck {
	boolean onCheck(MyHttpClient myHttpClient, int retry, String result);
}
