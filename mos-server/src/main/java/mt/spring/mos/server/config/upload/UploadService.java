package mt.spring.mos.server.config.upload;

import com.alibaba.fastjson.JSONObject;
import mt.spring.mos.server.config.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Martin
 * @Date 2020/6/26
 */
@Service
public class UploadService {
	@Autowired
	private RedisUtils redisUtils;
	private Map<String, UploadTotalProcess> cache = new ConcurrentHashMap<>();
	
	public UploadTotalProcess setUploadTotalProcess(HttpServletRequest request, UploadTotalProcess uploadTotalProcess) {
		if (uploadTotalProcess == null) {
			uploadTotalProcess = new UploadTotalProcess();
		}
		String uploadId = request.getParameter("uploadId");
		set(uploadId, uploadTotalProcess);
		return uploadTotalProcess;
	}
	
	public UploadTotalProcess getUploadTotalProcess(HttpServletRequest request) {
		String uploadId = request.getParameter("uploadId");
		return get(uploadId);
	}
	
	
	private void set(String uploadId, UploadTotalProcess uploadTotalProcess) {
		String json = JSONObject.toJSONString(uploadTotalProcess);
		redisUtils.set("upload-process-" + uploadId, json, 600000);
//		cache.put("upload-process-" + uploadId, uploadTotalProcess);
	}
	
	private UploadTotalProcess get(String uploadId) {
		String json = (String) redisUtils.get("upload-process-" + uploadId);
		if (json != null) {
			return JSONObject.parseObject(json, UploadTotalProcess.class);
		}
		return null;
//		return cache.get("upload-process-" + uploadId);
	}
}
