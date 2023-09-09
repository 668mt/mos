//package mt.spring.mos.sdk.utils;
//
//import lombok.extern.slf4j.Slf4j;
//import mt.spring.mos.base.stream.LimitInputStream;
//import mt.spring.mos.base.utils.SizeUtils;
//import mt.spring.mos.base.utils.TimeUtils;
//import mt.spring.mos.sdk.upload.TaskTimeWatch;
//import org.apache.commons.io.FileUtils;
//import org.apache.http.client.methods.CloseableHttpResponse;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.util.concurrent.TimeUnit;
//
///**
// * @Author Martin
// * @Date 2023/9/1
// */
//@Slf4j
//public class DownloadUtils {
//	public void downloadFile(String url, File desFile, boolean cover, long limitSpeedKbSeconds) throws IOException {
//		log.info("下载文件,url:{}，path:{}", url, desFile.getAbsolutePath());
//		File parentFile = desFile.getParentFile();
//		if (!parentFile.exists()) {
//			parentFile.mkdirs();
//		}
//		TaskTimeWatch taskTimeWatch = new TaskTimeWatch();
//		taskTimeWatch.start();
//		long lastModified = fileInfo.getLastModified() == null ? 0 : fileInfo.getLastModified();
//		Long length = fileInfo.getSizeByte();
//		File tempFile = new File(desFile.getPath() + ".tmp");
//		CloseableHttpResponse response = client.get(url);
//		try (InputStream inputStream = response.getEntity().getContent();
//			 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
//			if (limitSpeedKbSeconds > 0) {
//				try (LimitInputStream limitInputStream = new LimitInputStream(inputStream, limitSpeedKbSeconds)) {
//					org.apache.commons.io.IOUtils.copy(limitInputStream, outputStream);
//				}
//			} else {
//				org.apache.commons.io.IOUtils.copy(inputStream, outputStream);
//			}
//		}
//		if (cover && desFile.exists()) {
//			desFile.delete();
//		}
//		FileUtils.moveFile(tempFile, desFile);
//		desFile.setLastModified(lastModified);
//		taskTimeWatch.end();
//		long costMills = taskTimeWatch.getCostMills();
//		log.info("{}下载完成，用时：{}，平均下载速度：{}", pathname, TimeUtils.getReadableTime(costMills), getSpeed(length, costMills));
//	}
//
//	private String getSpeed(long length, long costMills) {
//		long seconds = TimeUnit.MILLISECONDS.toSeconds(costMills);
//		if (seconds <= 0) {
//			seconds = 1;
//		}
//		long speed = BigDecimal.valueOf(length).divide(BigDecimal.valueOf(seconds), 0, RoundingMode.HALF_UP).longValue();
//		return SizeUtils.getReadableSize(speed) + "/s";
//	}
//}
