package mt.utils.http;

import java.util.List;
import java.util.Map;

public interface OnCheck {
	boolean onCheck(int retry, String result, Map<String, List<String>> responseHeaders);
}
