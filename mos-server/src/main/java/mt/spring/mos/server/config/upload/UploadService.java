package mt.spring.mos.server.config.upload;

import com.alibaba.fastjson.JSONObject;
import mt.spring.mos.server.config.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author Martin
 * @Date 2020/6/26
 */
@Service
public class UploadService {
	@Autowired
	private RedisUtils redisUtils;
	
	public UploadTotalProcess setUploadTotalProcess(String uploadId, UploadTotalProcess uploadTotalProcess) {
		if (uploadTotalProcess == null) {
			uploadTotalProcess = new UploadTotalProcess();
		}
		set(uploadId, uploadTotalProcess);
		return uploadTotalProcess;
	}
	
	public UploadTotalProcess getUploadTotalProcess(String uploadId) {
		return get(uploadId);
	}
	
	
	private void set(String uploadId, UploadTotalProcess uploadTotalProcess) {
		String json = JSONObject.toJSONString(uploadTotalProcess);
		redisUtils.set("upload-process-" + uploadId, json, 600000);
	}
	
	private UploadTotalProcess get(String uploadId) {
		String json = (String) redisUtils.get("upload-process-" + uploadId);
		if (json != null) {
			return JSONObject.parseObject(json, UploadTotalProcess.class);
		}
		return null;
	}
}
