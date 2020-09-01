package mt.spring.mos.server.config.upload;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.ProgressListener;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @Author Martin
 * @Date 2019/12/28
 */
@Slf4j
public class SessionProcessListener implements ProgressListener {
	private final HttpServletRequest request;
	private final UploadService uploadService;
	private UploadTotalProcess uploadTotalProcess;
	
	public SessionProcessListener(HttpServletRequest request, UploadService uploadService) {
		this.request = request;
		this.uploadService = uploadService;
	}
	
	public UploadTotalProcess getUploadTotalProcess() {
		if (this.uploadTotalProcess == null) {
			this.uploadTotalProcess = uploadService.getUploadTotalProcess(request);
			if (uploadTotalProcess == null) {
				uploadTotalProcess = new UploadTotalProcess();
				uploadTotalProcess.addUpProcess(UploadTotalProcess.MULTIPART_UPLOAD_NAME, 1);
			}
		}
		return this.uploadTotalProcess;
	}
	
	private double lastPercent = 0;
	
	@Override
	public void update(long bytesRead, long contentLength, int items) {
		if (request == null) {
			return;
		}
		double percent = BigDecimal.valueOf(bytesRead * 1.0 / contentLength).setScale(2, RoundingMode.HALF_UP).doubleValue();
		if (percent != lastPercent) {
			UploadTotalProcess uploadTotalProcess = getUploadTotalProcess();
			uploadTotalProcess.updateProcess(UploadTotalProcess.MULTIPART_UPLOAD_NAME, percent);
			this.lastPercent = percent;
			log.debug("item" + items + ",total=" + contentLength + ",percent=" + percent);
			uploadService.setUploadTotalProcess(request, uploadTotalProcess);
		}
	}
}
