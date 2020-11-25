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
import mt.spring.mos.sdk.utils.Assert;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static mt.spring.mos.base.utils.ReflectUtils.getValue;

/**
 * @Author Martin
 * @Date 2020/11/25
 */
@Slf4j
public class UploadOperation {
	private final MosConfig mosConfig;
	private final UploadConfig uploadConfig;
	private final MosSdk mosSdk;
	private final ThreadPoolExecutor executorService;
	private final ServiceClient client;
	
	public UploadOperation(MosSdk mosSdk, MosConfig mosConfig, UploadConfig uploadConfig, ServiceClient client) {
		this.mosSdk = mosSdk;
		this.mosConfig = mosConfig;
		this.uploadConfig = uploadConfig;
		this.client = client;
		executorService = new ThreadPoolExecutor(uploadConfig.getThreadPoolCore(), uploadConfig.getThreadPoolCore(), 0, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
	}
	
	public void shutdown() {
		executorService.shutdown();
	}
	
	
	@Data
	class Task implements Runnable {
		private IOUtils.SplitResult splitResult;
		private int chunkIndex;
		private String pathname;
		private Boolean cover;
		private String sign;
		private UploadProcessListener uploadProcessListener;
		private InitUploadResult initUploadResult;
		
		public Task(InitUploadResult initUploadResult, IOUtils.SplitResult splitResult, int chunkIndex, String pathname, Boolean cover, String sign, UploadProcessListener uploadProcessListener) {
			this.initUploadResult = initUploadResult;
			this.splitResult = splitResult;
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
			Thread.currentThread().setName(pathname + "-" + chunkIndex);
			long totalSize = splitResult.getTotalSize();
			String totalMd5 = splitResult.getTotalMd5();
			IOUtils.UploadPart uploadPart = splitResult.getUploadParts().get(chunkIndex);
			InputStream inputStream = uploadPart.getInputStream();
			try {
				String chunkMd5 = DigestUtils.md5Hex(inputStream);
				log.debug("上传分片{},md5={},length={}", chunkIndex, chunkMd5, uploadPart.getLength());
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
		InitUploadResult initUploadResult = initUpload(new UploadInitRequest(totalMd5, totalSize, chunks, uploadInfo), sign);
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
	
	public void uploadFile(File file, UploadInfo uploadInfo, @Nullable UploadProcessListener uploadProcessListener) throws IOException {
		uploadFile(new FileInputStream(file), uploadInfo, uploadProcessListener);
	}
	
	private void uploadFile(FileInputStream fileInputStream, UploadInfo uploadInfo, @Nullable UploadProcessListener uploadProcessListener) throws IOException {
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
			IOUtils.SplitResult splitResult = mt.spring.mos.base.utils.IOUtils.splitFile(file, uploadConfig.getMinPartSize(), uploadConfig.getMaxPartSize(), uploadConfig.getExpectChunks());
			List<IOUtils.UploadPart> uploadParts = splitResult.getUploadParts();
			long partSize = splitResult.getPartSize();
			int chunks = uploadParts.size();
			long totalSize = splitResult.getTotalSize();
			
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
				futures.add(executorService.submit(new Task(initUploadResult, splitResult, i, pathname, uploadInfo.isCover(), sign, uploadProcessListener)));
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
}
