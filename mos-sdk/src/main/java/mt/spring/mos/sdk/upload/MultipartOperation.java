package mt.spring.mos.sdk.upload;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.utils.IOUtils;
import mt.spring.mos.base.utils.SizeUtils;
import mt.spring.mos.sdk.MosSdk;
import mt.spring.mos.sdk.entity.InitUploadResult;
import mt.spring.mos.sdk.entity.MosConfig;
import mt.spring.mos.sdk.entity.upload.*;
import mt.spring.mos.sdk.exception.UploadException;
import mt.spring.mos.sdk.http.ServiceClient;
import mt.spring.mos.sdk.interfaces.RecordFile;
import mt.spring.mos.sdk.utils.Assert;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHeader;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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
	private ThreadPoolExecutor singleExecutorService;
	private final ServiceClient client;
	
	public MultipartOperation(MosSdk mosSdk, MosConfig mosConfig, MosUploadConfig mosUploadConfig, ServiceClient client) {
		this.mosSdk = mosSdk;
		this.mosConfig = mosConfig;
		this.mosUploadConfig = mosUploadConfig;
		this.client = client;
	}
	
	public ThreadPoolExecutor getThreadPoolExecutor() {
		if (singleExecutorService == null) {
			synchronized (this) {
				if (singleExecutorService == null) {
					singleExecutorService = new ThreadPoolExecutor(mosUploadConfig.getThreadPoolCore(), mosUploadConfig.getThreadPoolCore(), 0, TimeUnit.SECONDS, new LinkedBlockingDeque<>(mosUploadConfig.getMaxQueueSize()));
				}
			}
		}
		return singleExecutorService;
	}
	
	public void shutdown() {
		if (singleExecutorService != null) {
			singleExecutorService.shutdown();
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
				log.debug("上传分片{}-{},md5={},length={}", pathname, chunkIndex, chunkMd5, uploadPart.getLength());
				inputStream.reset();
				UploadPartRequest uploadPartRequest = new UploadPartRequest(pathname, totalMd5, totalSize, chunkMd5, chunkIndex, inputStream, uploadPart.getLength());
				String host = mosConfig.getHost();
				String bucketName = mosConfig.getBucketName();
				String uploadUrl = host + "/upload/" + bucketName + "?sign=" + sign;
				CloseableHttpResponse closeableHttpResponse = client.post(uploadUrl, uploadPartRequest.buildEntity());
				client.checkSuccessAndGetResult(closeableHttpResponse, JSONObject.class);
				log.debug("分片" + chunkIndex + "，上传成功!");
			} catch (Exception e) {
				log.error("分片" + chunkIndex + "上传失败：" + e.getMessage(), e);
				throw new RuntimeException(e);
			} finally {
				if (uploadProcessListener != null) {
					uploadProcessListener.addDone();
				}
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
		String sign = mosSdk.getSign(pathname, 2, TimeUnit.HOURS);
		String host = mosConfig.getHost();
		String bucketName = mosConfig.getBucketName();
		String totalMd5 = UUID.randomUUID().toString();
		long totalSize = 0;
		int chunks = 0;
		
		TaskTimeWatch taskTimeWatch = new TaskTimeWatch(pathname + "上传");
		taskTimeWatch.start();
		InitUploadResult initUploadResult = initUpload(new UploadInitRequest(totalMd5, totalSize, 1, uploadInfo), sign);
		boolean md5Exists = initUploadResult.isFileExists();
		if (md5Exists) {
			log.info("{}上传完成", pathname);
			taskTimeWatch.end();
			return;
		}
		chunks = IOUtils.convertStreamToByteBufferStream(originInputStream, (inputStream, chunkIndex) -> {
			try {
				String chunkMd5 = DigestUtils.md5Hex(inputStream);
				log.debug("上传分片:" + chunkIndex + ":" + chunkMd5);
				inputStream.reset();
				UploadPartRequest uploadPartRequest = new UploadPartRequest(pathname, totalMd5, totalSize, chunkMd5, chunkIndex, inputStream, inputStream.available());
				String uploadUrl = host + "/upload/" + bucketName + "?sign=" + sign;
				CloseableHttpResponse closeableHttpResponse = client.post(uploadUrl, uploadPartRequest.buildEntity());
				client.checkSuccessAndGetResult(closeableHttpResponse, JSONObject.class);
				log.debug("分片" + chunkIndex + "，上传成功!");
			} catch (Exception e) {
				log.error("分片" + chunkIndex + "上传失败：" + e.getMessage(), e);
				throw new RuntimeException(e);
			}
		});
		UploadMergeRequest uploadMergeRequest = new UploadMergeRequest(totalMd5, totalSize, chunks, true, false, uploadInfo);
		merge(uploadMergeRequest, sign);
		taskTimeWatch.end();
	}
	
	private InitUploadResult initUpload(UploadInitRequest uploadInitRequest, String sign) throws IOException {
		String host = mosConfig.getHost();
		String bucketName = mosConfig.getBucketName();
		String url = host + "/upload/" + bucketName + "/init?sign=" + sign;
		return client.checkSuccessAndGetResult(client.post(url, uploadInitRequest.buildEntity()), InitUploadResult.class);
	}
	
	public void uploadFile(File file, UploadInfo uploadInfo, @Nullable UploadProcessListener uploadProcessListener) throws
			IOException {
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
		TaskTimeWatch taskTimeWatch = new TaskTimeWatch(pathname + "上传");
		taskTimeWatch.start();
		String sign = mosSdk.getSign(pathname, 2, TimeUnit.HOURS);
		try {
			IOUtils.FileSplitResult fileSplitResult = mt.spring.mos.base.utils.IOUtils.splitFile(file, mosUploadConfig.getMinPartSize(), mosUploadConfig.getMaxPartSize(), mosUploadConfig.getExpectChunks());
			List<IOUtils.UploadPart> uploadParts = fileSplitResult.getUploadParts();
			long partSize = fileSplitResult.getPartSize();
			int chunks = uploadParts.size();
			long totalSize = fileSplitResult.getTotalSize();
			
			log.info("分片数：" + chunks + ",分片大小：" + SizeUtils.getReadableSize(partSize));
			String totalMd5 = DigestUtils.md5Hex(fileInputStream);
			InitUploadResult initUploadResult = initUpload(new UploadInitRequest(totalMd5, totalSize, chunks, uploadInfo), sign);
			boolean md5Exists = initUploadResult.isFileExists();
			if (md5Exists) {
				log.info("{}上传完成", pathname);
				taskTimeWatch.end();
				return;
			}
			if (uploadProcessListener != null) {
				uploadProcessListener.init(chunks);
			}
			List<Future<?>> futures = new ArrayList<>();
			for (int i = 0; i < chunks; i++) {
				futures.add(getThreadPoolExecutor().submit(new Task(initUploadResult, fileSplitResult, i, pathname, uploadInfo.isCover(), sign, uploadProcessListener)));
			}
			for (Future<?> future : futures) {
				try {
					future.get();
				} catch (ExecutionException e) {
					throw new UploadException(e);
				}
			}
			UploadMergeRequest uploadMergeRequest = new UploadMergeRequest(totalMd5, totalSize, chunks, false, false, uploadInfo);
			merge(uploadMergeRequest, sign);
			taskTimeWatch.end();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			fileInputStream.close();
			if (uploadProcessListener != null) {
				uploadProcessListener.finish();
			}
		}
	}
	
	private void merge(UploadMergeRequest uploadMergeRequest, String sign) throws IOException {
		log.info("合并中...");
		String host = mosSdk.getMosConfig().getHost();
		String bucketName = mosSdk.getMosConfig().getBucketName();
		String pathname = uploadMergeRequest.getPathname();
		String mergeUrl = host + "/upload/mergeFiles?bucketName=" + bucketName + "&sign=" + sign;
		CloseableHttpResponse mergeResponse = client.post(mergeUrl, uploadMergeRequest.buildEntity());
		client.checkSuccessAndGetResult(mergeResponse, JSONObject.class);
		log.info("{}上传完成!", pathname);
	}
	
	private void checkUploadInfo(UploadInfo uploadInfo) {
		Assert.notNull(uploadInfo, "uploadInfo can not be null");
		String pathname = mosSdk.checkPathname(uploadInfo.getPathname());
		uploadInfo.setPathname(pathname);
	}
	
	public class DownloadTask implements Runnable {
		private final String url;
		private final String pathname;
		private final IOUtils.SplitPart part;
		private final File tempFile;
		private final RecordFile recordFile;
		
		public DownloadTask(RecordFile recordFile, String pathname, String url, IOUtils.SplitPart part, File tempFile) {
			this.recordFile = recordFile;
			this.pathname = pathname;
			this.url = url;
			this.part = part;
			this.tempFile = tempFile;
		}
		
		@Override
		public void run() {
			RandomAccessFile randomAccessFile = null;
			InputStream inputStream = null;
			try {
				log.debug("[{}]下载分片{}...", pathname, part.getIndex());
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
				log.debug("[{}]分片{}下载完成！", pathname, part.getIndex());
				recordFile.finish(part.getIndex());
			} catch (IOException e) {
				throw new RuntimeException("下载" + pathname + "分片" + part.getIndex() + "失败", e);
			} finally {
				org.apache.commons.io.IOUtils.closeQuietly(randomAccessFile);
				org.apache.commons.io.IOUtils.closeQuietly(inputStream);
			}
			
		}
	}
	
	public void downloadFile(String pathname, File desFile) throws IOException {
		downloadFile(pathname, desFile, false);
	}
	
	public void downloadFile(String pathname, File desFile, boolean cover) throws IOException {
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		Assert.state(mosSdk.isExists(pathname), "不存在资源" + pathname);
		log.info("下载文件：{} -> {}", pathname, desFile);
		TaskTimeWatch taskTimeWatch = new TaskTimeWatch("下载文件[" + pathname + "]");
		taskTimeWatch.start();
		String url = mosSdk.getUrl(pathname, 30, TimeUnit.SECONDS);
		CloseableHttpResponse response = client.get(url);
		long length = Long.parseLong(response.getFirstHeader("content-length").getValue());
		Header lastModifiedHeader = response.getFirstHeader("last-modified");
		String lastModified = lastModifiedHeader != null ? lastModifiedHeader.getValue() : "0";
		File tempFile = new File(desFile.getPath() + ".tmp");
		RecordFile recordFile = null;
		if (length > mosUploadConfig.getMinPartSize()) {
			IOUtils.SplitResult splitResult = IOUtils.split(length, mosUploadConfig.getMinPartSize(), mosUploadConfig.getMaxPartSize(), mosUploadConfig.getExpectChunks());
			recordFile = new PropertiesRecordFile(pathname, lastModified, splitResult.getChunks());
			log.info("文件[{}]分片数：{}，分片大小：{}", pathname, splitResult.getChunks(), SizeUtils.getReadableSize(splitResult.getPartSize()));
			String finalPathname = pathname;
			RecordFile finalRecordFile = recordFile;
			List<? extends Future<?>> futures = splitResult.getSplitParts().stream()
					.filter(part -> !finalRecordFile.hasDownload(part.getIndex()))
					.map(part -> getThreadPoolExecutor().submit(new DownloadTask(finalRecordFile, finalPathname, url, part, tempFile))).collect(Collectors.toList());
			for (Future<?> future : futures) {
				try {
					future.get();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		} else {
			try (InputStream content = response.getEntity().getContent();
				 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
				org.apache.commons.io.IOUtils.copy(content, outputStream);
			}
		}
		if (cover && desFile.exists()) {
			desFile.delete();
		}
		FileUtils.moveFile(tempFile, desFile);
		if (recordFile != null) {
			recordFile.clear();
		}
		log.info("{}下载完成，目标文件:{}", pathname, desFile);
		taskTimeWatch.end();
	}
}
