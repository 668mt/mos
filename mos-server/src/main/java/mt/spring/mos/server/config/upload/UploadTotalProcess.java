package mt.spring.mos.server.config.upload;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2020/6/26
 */
@Data
public class UploadTotalProcess {
	private Map<String, UploadProcess> processMap = new HashMap<>();
	public static final String MULTIPART_UPLOAD_NAME = "multipart-upload";
	private String uploadId;
	
	public UploadTotalProcess addUpProcess(String name, double weight) {
		processMap.put(name, new UploadProcess(name, weight));
		return this;
	}
	
	public void updateProcess(String name, double percent) {
		UploadProcess uploadProcess = processMap.get(name);
		if (uploadProcess != null) {
			uploadProcess.setPercent(percent);
		}
	}
	
	public double getPercent() {
		double totalPercent = 0;
		for (Map.Entry<String, UploadProcess> stringUpProcessEntry : processMap.entrySet()) {
			UploadProcess value = stringUpProcessEntry.getValue();
			totalPercent += value.getWeightPercent();
		}
		return BigDecimal.valueOf(totalPercent).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}
}
