package mt.spring.mos.sdk.upload;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.utils.IOUtils;
import mt.spring.mos.base.utils.SizeUtils;
import mt.spring.mos.base.utils.TimeUtils;
import mt.spring.mos.sdk.entity.MosConfig;
import mt.spring.mos.sdk.http.ServiceClient;
import mt.spring.mos.sdk.interfaces.RecordFile;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHeader;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static mt.spring.mos.base.utils.IOUtils.MB;

/**
 * @Author Martin
 * @Date 2022/10/29
 */
@Slf4j
public class Mp4DownloadTest {
	private static final ExecutorService executorService = Executors.newFixedThreadPool(5);
	private static final ServiceClient client = new ServiceClient(new MosConfig());
	/**
	 * 最小的分片大小，单位byte，默认5MB
	 */
	private static final long minPartSize = 5 * MB;
	/**
	 * 最大的分片大小，单位byte，默认20MB
	 */
	private static final long maxPartSize = 20 * MB;
	private static final int expectChunks = 50;
	
	public static void main(String[] args) throws IOException {
		System.setProperty("java.io.tmpdir", "D:/test");
		String url = "http://192.168.0.2:4100/mos/default/resources/mucanpp/Elena-%E7%AE%A1%E5%AE%B6.mp4?sign=yTT7PvYUViRUVUxy2yGKRlfj0C7OC_prflYxEjZowrJ_iDoGjs0td5wOIAEFjUcgDzClMgfmCxpuHQmCXNd2EHkl5cFcmS3sAO9UGbPxWhi2s0EVZX6CPoSosQDPJvdjvUT48HxjDcT5D6kIbUCn6-QG3T7_upvuSUQjhGO76V45mLgEHSDuBC0lAfU0bwtnYkbud0palKYuCWFQceR-PHdLw==";
		Mp4DownloadTest mp4DownloadTest = new Mp4DownloadTest();
		mp4DownloadTest.downloadFile(url, new File("D:/test/test-multi.mp4"));
		executorService.shutdownNow();
		client.shutdown();
	}
	
	public void downloadFile(String url, File desFile) throws IOException {
		String name = desFile.getName();
		CloseableHttpResponse response = client.get(url);
		long length = 0;
		if (response.containsHeader("content-length")) {
			length = Long.parseLong(response.getFirstHeader("content-length").getValue());
		}
		log.info("下载文件：dstFile:{},url:{}", desFile.getAbsolutePath(), url);
		File parentFile = desFile.getParentFile();
		if (!parentFile.exists()) {
			parentFile.mkdirs();
		}
		
		TaskTimeWatch taskTimeWatch = new TaskTimeWatch();
		taskTimeWatch.start();
		File tempFile = new File(desFile.getPath() + ".tmp");
		RecordFile recordFile = null;
		if (length > minPartSize) {
			log.info("使用多线程下载");
			log.debug("临时文件路径：{}", FileUtils.getTempDirectoryPath());
			IOUtils.SplitResult splitResult = IOUtils.split(length, minPartSize, maxPartSize, expectChunks);
			recordFile = new PropertiesRecordFile(url);
			log.debug("文件[{}]分片数：{}，分片大小：{}", name, splitResult.getChunks(), SizeUtils.getReadableSize(splitResult.getPartSize()));
			RecordFile finalRecordFile = recordFile;
			List<? extends Future<?>> futures = splitResult.getSplitParts().stream()
					.map(part -> executorService.submit(new DownloadTask(finalRecordFile, url, name, part, tempFile)))
					.collect(Collectors.toList());
			for (Future<?> future : futures) {
				try {
					future.get();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		} else {
			log.info("使用单线程下载");
			try (InputStream content = response.getEntity().getContent();
				 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
				org.apache.commons.io.IOUtils.copy(content, outputStream);
			}
		}
		if (desFile.exists()) {
			desFile.delete();
		}
		FileUtils.moveFile(tempFile, desFile);
		taskTimeWatch.end();
		long costMills = taskTimeWatch.getCostMills();
		log.info("{}下载完成，用时：{}，平均下载速度：{}", name, TimeUtils.getReadableTime(costMills), getSpeed(length, costMills));
		if (recordFile != null) {
			recordFile.clear();
		}
	}
	
	private static String getSpeed(long length, long costMills) {
		long seconds = TimeUnit.MILLISECONDS.toSeconds(costMills);
		if (seconds <= 0) {
			seconds = 1;
		}
		long speed = BigDecimal.valueOf(length).divide(BigDecimal.valueOf(seconds), 0, RoundingMode.HALF_UP).longValue();
		return SizeUtils.getReadableSize(speed) + "/s";
	}
	
	public static class DownloadTask implements Runnable {
		private final String url;
		private final String name;
		private final IOUtils.SplitPart part;
		private final File tempFile;
		private final RecordFile recordFile;
		
		public DownloadTask(RecordFile recordFile, String url, String name, IOUtils.SplitPart part, File tempFile) {
			this.recordFile = recordFile;
			this.url = url;
			this.name = name;
			this.part = part;
			this.tempFile = tempFile;
		}
		
		@Override
		public void run() {
			if (recordFile.hasDownload(part.getIndex())) {
				log.info("part{}已经下载，跳过", part.getIndex());
				return;
			}
			RandomAccessFile randomAccessFile = null;
			InputStream inputStream = null;
			try {
				TaskTimeWatch taskTimeWatch = new TaskTimeWatch();
				taskTimeWatch.start();
				log.trace("[{}]下载分片{}...", name, part.getIndex());
				BasicHeader basicHeader = new BasicHeader("Range", "bytes=" + part.getStart() + "-" + part.getEnd());
				CloseableHttpResponse response = client.get(url, basicHeader);
				inputStream = response.getEntity().getContent();
				randomAccessFile = new RandomAccessFile(tempFile, "rw");
				randomAccessFile.seek(part.getStart());
				byte[] buffer = new byte[4096];
				int read;
				while ((read = inputStream.read(buffer)) != -1) {
					randomAccessFile.write(buffer, 0, read);
				}
				long length = part.getLength();
				taskTimeWatch.end();
				long costMills = taskTimeWatch.getCostMills();
				log.info("[{}]分片{}下载完成，用时：{},平均下载速度：{}", name, part.getIndex(), TimeUtils.getReadableTime(costMills), getSpeed(length, costMills));
				recordFile.finish(part.getIndex());
			} catch (IOException e) {
				throw new RuntimeException("下载" + name + "分片" + part.getIndex() + "失败", e);
			} finally {
				IOUtils.closeQuietly(randomAccessFile);
				IOUtils.closeQuietly(inputStream);
			}
		}
		
	}
}
