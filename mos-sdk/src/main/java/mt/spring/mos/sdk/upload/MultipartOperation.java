package mt.spring.mos.sdk.upload;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.base.utils.IOUtils;
import mt.spring.mos.base.utils.SizeUtils;
import mt.spring.mos.base.utils.TimeUtils;
import mt.spring.mos.sdk.MosSdk;
import mt.spring.mos.sdk.entity.InitUploadResult;
import mt.spring.mos.sdk.entity.MosConfig;
import mt.spring.mos.sdk.entity.Resource;
import mt.spring.mos.sdk.entity.upload.*;
import mt.spring.mos.sdk.exception.UploadException;
import mt.spring.mos.sdk.http.ServiceClient;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static mt.spring.mos.base.utils.ReflectUtils.getValue;

/**
 * @Author Martin
 * @Date 2020/11/25
 */
@Slf4j
public class MultipartOperation {
	private final MosConfig mosConfig;
	private MosUploadConfig mosUploadConfig;
	private final MosSdk mosSdk;
	private ThreadPoolExecutor uploadExecutor;
	private ThreadPoolExecutor downloadExecutor;
	private final ServiceClient client;
	
	public MultipartOperation(MosSdk mosSdk, MosConfig mosConfig, MosUploadConfig mosUploadConfig, ServiceClient client) {
		this.mosSdk = mosSdk;
		this.mosConfig = mosConfig;
		this.mosUploadConfig = mosUploadConfig;
		this.client = client;
	}
	
	public ThreadPoolExecutor getUploadExecutor() {
		if (uploadExecutor == null) {
			synchronized (this) {
				if (uploadExecutor == null) {
					uploadExecutor = new ThreadPoolExecutor(mosUploadConfig.getThreadPoolCore(), mosUploadConfig.getThreadPoolCore(), 0, TimeUnit.SECONDS, new LinkedBlockingDeque<>(mosUploadConfig.getMaxQueueSize()));
				}
			}
		}
		return uploadExecutor;
	}
	
	public ThreadPoolExecutor getDownloadExecutor() {
		if (downloadExecutor == null) {
			synchronized (this) {
				if (downloadExecutor == null) {
					downloadExecutor = new ThreadPoolExecutor(mosUploadConfig.getThreadPoolCore(), mosUploadConfig.getThreadPoolCore(), 0, TimeUnit.SECONDS, new LinkedBlockingDeque<>(mosUploadConfig.getMaxQueueSize()));
				}
			}
		}
		return downloadExecutor;
	}
	
	public void shutdown() {
		if (uploadExecutor != null) {
			uploadExecutor.shutdown();
			uploadExecutor = null;
		}
		if (downloadExecutor != null) {
			downloadExecutor.shutdown();
			downloadExecutor = null;
		}
	}
	
	public void setMosUploadConfig(MosUploadConfig mosUploadConfig) {
		this.mosUploadConfig = mosUploadConfig;
	}
	
	
	@Data
	class Task implements Runnable {
		private IOUtils.FileSplitResult fileSplitResult;
		private int chunkIndex;
		private String pathname;
		private Boolean cover;
		private String sign;
		private UploadProcessListener uploadProcessListener;
		private InitUploadResult initUploadResult;
		
		public Task(InitUploadResult initUploadResult, IOUtils.FileSplitResult fileSplitResult, int chunkIndex, String pathname, Boolean cover, String sign, UploadProcessListener uploadProcessListener) {
			this.initUploadResult = initUploadResult;
			this.fileSplitResult = fileSplitResult;
			this.chunkIndex = chunkIndex;
			this.pathname = pathname;
			this.cover = cover;
			this.sign = sign;
			this.uploadProcessListener = uploadProcessListener;
		}
		
		@Override
		public void run() {
			if (initUploadResult.hasUploaded(chunkIndex)) {
				if (uploadProcessListener != null) {
					uploadProcessListener.addDone();
				}
				return;
			}
			long totalSize = fileSplitResult.getTotalSize();
			String totalMd5 = fileSplitResult.getTotalMd5();
			IOUtils.UploadPart uploadPart = fileSplitResult.getUploadParts().get(chunkIndex);
			InputStream inputStream = uploadPart.getInputStream();
			try {
				String chunkMd5 = DigestUtils.md5Hex(inputStream);
				log.trace("上传分片{}-{},md5={},length={}", pathname, chunkIndex, chunkMd5, uploadPart.getLength());
				inputStream.reset();
				UploadPartRequest uploadPartRequest = new UploadPartRequest(pathname, totalMd5, totalSize, chunkMd5, chunkIndex, inputStream, uploadPart.getLength());
				String host = mosConfig.getHost();
				String bucketName = mosConfig.getBucketName();
				String uploadUrl = host + "/upload/" + bucketName + "?sign=" + sign;
				CloseableHttpResponse closeableHttpResponse = client.post(uploadUrl, uploadPartRequest.buildEntity());
				client.checkSuccessAndGetResult(closeableHttpResponse, JSONObject.class);
				log.trace("分片{}-{}，上传成功!", pathname, chunkIndex);
			} catch (Exception e) {
				log.error("分片" + chunkIndex + "上传失败：" + e.getMessage(), e);
				throw new RuntimeException(e);
			} finally {
				if (uploadProcessListener != null) {
					uploadProcessListener.addDone();
				}
				IOUtils.closeQuietly(inputStream);
			}
		}
	}
	
	public void uploadStream(InputStream originInputStream, UploadInfo uploadInfo) throws IOException {
		checkUploadInfo(uploadInfo);
		String pathname = uploadInfo.getPathname();
		if (originInputStream instanceof FileInputStream) {
			uploadFile((FileInputStream) originInputStream, uploadInfo, null);
			return;
		} else if (originInputStream instanceof FilterInputStream) {
			try {
				InputStream in = (InputStream) getValue(originInputStream, "in");
				if (in instanceof FileInputStream) {
					uploadFile((FileInputStream) in, uploadInfo, null);
					return;
				}
			} catch (NoSuchFieldException e) {
				throw new RuntimeException(e);
			}
		}
		long lastModified = System.currentTimeMillis();
		String sign = mosSdk.getSign(pathname, 2, TimeUnit.HOURS);
		String host = mosConfig.getHost();
		String bucketName = mosConfig.getBucketName();
		String totalMd5 = UUID.randomUUID().toString();
		long totalSize = 0;
		int chunks;
		
		log.info("上传{}...", pathname);
		TaskTimeWatch taskTimeWatch = new TaskTimeWatch();
		taskTimeWatch.start();
		AtomicLong size = new AtomicLong(0);
		try {
			InitUploadResult initUploadResult = initUpload(new UploadInitRequest(totalMd5, totalSize, 1, lastModified, uploadInfo), sign);
			boolean md5Exists = initUploadResult.isFileExists();
			if (md5Exists) {
				log.debug("秒传{}", pathname);
				taskTimeWatch.end();
				return;
			}
			chunks = IOUtils.convertStreamToByteBufferStream(originInputStream, (inputStream, chunkIndex) -> {
				try {
					String chunkMd5 = DigestUtils.md5Hex(inputStream);
					log.trace("上传分片{}:{}", chunkIndex, chunkMd5);
					inputStream.reset();
					int length = inputStream.available();
					size.addAndGet(length);
					UploadPartRequest uploadPartRequest = new UploadPartRequest(pathname, totalMd5, totalSize, chunkMd5, chunkIndex, inputStream, length);
					String uploadUrl = host + "/upload/" + bucketName + "?sign=" + sign;
					CloseableHttpResponse closeableHttpResponse = client.post(uploadUrl, uploadPartRequest.buildEntity());
					client.checkSuccessAndGetResult(closeableHttpResponse, JSONObject.class);
					log.trace("分片" + chunkIndex + "，上传成功!");
				} catch (Exception e) {
					log.error("分片" + chunkIndex + "上传失败：" + e.getMessage(), e);
					throw new RuntimeException(e);
				}
			});
			
			UploadMergeRequest uploadMergeRequest = new UploadMergeRequest(totalMd5, totalSize, chunks, true, uploadInfo.isWaitMerge(), lastModified, uploadInfo);
			merge(uploadMergeRequest, sign);
			taskTimeWatch.end();
		} finally {
			if (taskTimeWatch.getEnd() > 0) {
				long costMills = taskTimeWatch.getCostMills();
				log.info("{}上传完成，用时：{}，平均上传速度：{}", pathname, TimeUtils.getReadableTime(costMills), getSpeed(size.get(), costMills));
			}
		}
	}
	
	private InitUploadResult initUpload(UploadInitRequest uploadInitRequest, String sign) throws IOException {
		String host = mosConfig.getHost();
		String bucketName = mosConfig.getBucketName();
		String url = host + "/upload/" + bucketName + "/init?sign=" + sign;
		return client.checkSuccessAndGetResult(client.post(url, uploadInitRequest.buildEntity()), InitUploadResult.class);
	}
	
	public void uploadFile(File file, UploadInfo uploadInfo, @Nullable UploadProcessListener uploadProcessListener) throws
			IOException {
		Assert.state(file.exists() && file.isFile(), "文件" + file + "不是文件");
		uploadFile(new FileInputStream(file), uploadInfo, uploadProcessListener);
	}
	
	private void uploadFile(FileInputStream fileInputStream, UploadInfo uploadInfo, @Nullable UploadProcessListener
			uploadProcessListener) throws IOException {
		checkUploadInfo(uploadInfo);
		String pathname = uploadInfo.getPathname();
		String filePath;
		try {
			filePath = (String) getValue(fileInputStream, "path");
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
		File file = new File(filePath);
		TaskTimeWatch taskTimeWatch = new TaskTimeWatch();
		taskTimeWatch.start();
		String sign = mosSdk.getSign(pathname, 2, TimeUnit.HOURS);
		long lastModified = file.lastModified();
		IOUtils.FileSplitResult fileSplitResult = mt.spring.mos.base.utils.IOUtils.splitFile(file, mosUploadConfig.getMinPartSize(), mosUploadConfig.getMaxPartSize(), mosUploadConfig.getExpectChunks());
		long totalSize = fileSplitResult.getTotalSize();
		try {
			List<IOUtils.UploadPart> uploadParts = fileSplitResult.getUploadParts();
			long partSize = fileSplitResult.getPartSize();
			int chunks = uploadParts.size();
			
			log.info("上传{}，分片数：" + chunks + ",分片大小：" + SizeUtils.getReadableSize(partSize), pathname);
			String totalMd5 = fileSplitResult.getTotalMd5();
			InitUploadResult initUploadResult = initUpload(new UploadInitRequest(totalMd5, totalSize, chunks, lastModified, uploadInfo), sign);
			boolean md5Exists = initUploadResult.isFileExists();
			if (md5Exists) {
				log.debug("秒传{}", pathname);
				taskTimeWatch.end();
				return;
			}
			if (uploadProcessListener != null) {
				uploadProcessListener.init(chunks);
			}
			List<Future<?>> futures = new ArrayList<>();
			for (int i = 0; i < chunks; i++) {
				futures.add(getUploadExecutor().submit(new Task(initUploadResult, fileSplitResult, i, pathname, uploadInfo.isCover(), sign, uploadProcessListener)));
			}
			for (Future<?> future : futures) {
				try {
					future.get();
				} catch (ExecutionException e) {
					throw new UploadException(e);
				}
			}
			UploadMergeRequest uploadMergeRequest = new UploadMergeRequest(totalMd5, totalSize, chunks, false, uploadInfo.isWaitMerge(), lastModified, uploadInfo);
			log.debug("开始合并：{}", pathname);
			merge(uploadMergeRequest, sign);
			taskTimeWatch.end();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			if (taskTimeWatch.getEnd() > 0) {
				long costMills = taskTimeWatch.getCostMills();
				log.info("{}上传完成，用时：{},平均上传速度：{}", pathname, TimeUtils.getReadableTime(costMills), getSpeed(totalSize, costMills));
			}
			for (IOUtils.UploadPart uploadPart : fileSplitResult.getUploadParts()) {
				IOUtils.closeQuietly(uploadPart.getInputStream());
			}
			IOUtils.closeQuietly(fileInputStream);
			if (uploadProcessListener != null) {
				uploadProcessListener.finish();
			}
		}
	}
	
	private void merge(UploadMergeRequest uploadMergeRequest, String sign) throws IOException {
		log.debug("合并中...");
		String host = mosSdk.getMosConfig().getHost();
		String bucketName = mosSdk.getMosConfig().getBucketName();
		String mergeUrl = host + "/upload/mergeFiles?bucketName=" + bucketName + "&sign=" + sign;
		CloseableHttpResponse mergeResponse = client.post(mergeUrl, uploadMergeRequest.buildEntity());
		client.checkSuccessAndGetResult(mergeResponse, JSONObject.class);
	}
	
	private void checkUploadInfo(UploadInfo uploadInfo) {
		Assert.notNull(uploadInfo, "uploadInfo can not be null");
		String pathname = mosSdk.checkPathname(uploadInfo.getPathname());
		uploadInfo.setPathname(pathname);
	}
	
	public void downloadFile(String pathname, File desFile) throws IOException {
		downloadFile(pathname, desFile, false);
	}
	
	public void downloadFile(String pathname, File desFile, boolean cover) throws IOException {
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		Resource fileInfo = mosSdk.getFileInfo(pathname);
		Assert.notNull(fileInfo, "不存在资源" + pathname);
		log.info("下载文件：{} -> {}", pathname, desFile.getAbsolutePath());
		File parentFile = desFile.getParentFile();
		if (!parentFile.exists()) {
			parentFile.mkdirs();
		}
		TaskTimeWatch taskTimeWatch = new TaskTimeWatch();
		taskTimeWatch.start();
		long lastModified = fileInfo.getLastModified() == null ? 0 : fileInfo.getLastModified();
		Long length = fileInfo.getSizeByte();
		String url = mosSdk.getEncodedUrl(pathname, 30, TimeUnit.SECONDS);
		File tempFile = new File(desFile.getPath() + ".tmp");
		CloseableHttpResponse response = client.get(url);
		try (InputStream content = response.getEntity().getContent();
			 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
			org.apache.commons.io.IOUtils.copy(content, outputStream);
		}
		if (cover && desFile.exists()) {
			desFile.delete();
		}
		FileUtils.moveFile(tempFile, desFile);
		desFile.setLastModified(lastModified);
		taskTimeWatch.end();
		long costMills = taskTimeWatch.getCostMills();
		log.info("{}下载完成，用时：{}，平均下载速度：{}", pathname, TimeUtils.getReadableTime(costMills), getSpeed(length, costMills));
	}
//	public void downloadFile(String pathname, File desFile, boolean cover) throws IOException {
//		if (!pathname.startsWith("/")) {
//			pathname = "/" + pathname;
//		}
//		Resource fileInfo = mosSdk.getFileInfo(pathname);
//		Assert.notNull(fileInfo, "不存在资源" + pathname);
//		log.info("下载文件：{} -> {}", pathname, desFile.getAbsolutePath());
//		File parentFile = desFile.getParentFile();
//		if (!parentFile.exists()) {
//			parentFile.mkdirs();
//		}
//		TaskTimeWatch taskTimeWatch = new TaskTimeWatch();
//		taskTimeWatch.start();
//		long lastModified = fileInfo.getLastModified() == null ? 0 : fileInfo.getLastModified();
//		Long length = fileInfo.getSizeByte();
//		String url = mosSdk.getEncodedUrl(pathname, 30, TimeUnit.SECONDS);
//		File tempFile = new File(desFile.getPath() + ".tmp");
//		RecordFile recordFile = null;
//		if (length > mosUploadConfig.getMinPartSize()) {
//			log.debug("临时文件路径：{}", FileUtils.getTempDirectoryPath());
//			IOUtils.SplitResult splitResult = IOUtils.split(length, mosUploadConfig.getMinPartSize(), mosUploadConfig.getMaxPartSize(), mosUploadConfig.getExpectChunks());
//			recordFile = new PropertiesRecordFile(mosSdk.getMosConfig().getBucketName(), pathname, lastModified, splitResult.getChunks());
//			log.debug("文件[{}]分片数：{}，分片大小：{}", pathname, splitResult.getChunks(), SizeUtils.getReadableSize(splitResult.getPartSize()));
//			String finalPathname = pathname;
//			RecordFile finalRecordFile = recordFile;
//			List<? extends Future<?>> futures = splitResult.getSplitParts().stream()
////					.filter(part -> !finalRecordFile.hasDownload(part.getIndex()))
//					.map(part -> getThreadPoolExecutor().submit(new DownloadTask(finalRecordFile, finalPathname, url, part, tempFile)))
//					.collect(Collectors.toList());
//			for (Future<?> future : futures) {
//				try {
//					future.get();
//				} catch (Exception e) {
//					throw new RuntimeException(e);
//				}
//			}
//		} else {
//			CloseableHttpResponse response = client.get(url);
//			try (InputStream content = response.getEntity().getContent();
//				 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
//				org.apache.commons.io.IOUtils.copy(content, outputStream);
//			}
//		}
//		if (cover && desFile.exists()) {
//			desFile.delete();
//		}
//		FileUtils.moveFile(tempFile, desFile);
//		desFile.setLastModified(lastModified);
//		if (recordFile != null) {
//			recordFile.clear();
//		}
//		taskTimeWatch.end();
//		long costMills = taskTimeWatch.getCostMills();
//		log.info("{}下载完成，用时：{}，平均下载速度：{}", pathname, TimeUtils.getReadableTime(costMills), getSpeed(length, costMills));
//	}
	
	private String getSpeed(long length, long costMills) {
		long seconds = TimeUnit.MILLISECONDS.toSeconds(costMills);
		if (seconds <= 0) {
			seconds = 1;
		}
		long speed = BigDecimal.valueOf(length).divide(BigDecimal.valueOf(seconds), 0, RoundingMode.HALF_UP).longValue();
		return SizeUtils.getReadableSize(speed) + "/s";
	}
	
//	public class DownloadTask implements Runnable {
//		private final String url;
//		private final String pathname;
//		private final IOUtils.SplitPart part;
//		private final File tempFile;
//		private final RecordFile recordFile;
//
//		public DownloadTask(RecordFile recordFile, String pathname, String url, IOUtils.SplitPart part, File tempFile) {
//			this.recordFile = recordFile;
//			this.pathname = pathname;
//			this.url = url;
//			this.part = part;
//			this.tempFile = tempFile;
//		}
//
//		@Override
//		public void run() {
//			RandomAccessFile randomAccessFile = null;
//			InputStream inputStream = null;
//			try {
//				log.trace("[{}]下载分片{}...", pathname, part.getIndex());
//				BasicHeader basicHeader = new BasicHeader("Range", "bytes=" + part.getStart() + "-" + part.getEnd());
//				CloseableHttpResponse response = client.get(url, basicHeader);
//				inputStream = response.getEntity().getContent();
//				randomAccessFile = new RandomAccessFile(tempFile, "rw");
//				randomAccessFile.seek(part.getStart());
//				byte[] buffer = new byte[4096];
//				int read;
//				while ((read = inputStream.read(buffer)) != -1) {
//					randomAccessFile.write(buffer, 0, read);
//				}
//				log.trace("[{}]分片{}下载完成！", pathname, part.getIndex());
//				recordFile.finish(part.getIndex());
//			} catch (IOException e) {
//				throw new RuntimeException("下载" + pathname + "分片" + part.getIndex() + "失败", e);
//			} finally {
//				IOUtils.closeQuietly(randomAccessFile);
//				IOUtils.closeQuietly(inputStream);
//			}
//		}
//
//	}
}
