package mt.spring.mos.client.service;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.stream.BoundedInputStream;
import mt.spring.mos.base.stream.MosEncodeInputStream;
import mt.spring.mos.base.stream.MosEncodeOutputStream;
import mt.spring.mos.base.stream.RepeatableBoundedFileInputStream;
import mt.spring.mos.base.utils.MosFileEncodeUtils;
import mt.spring.mos.client.entity.MergeResult;
import mt.spring.mos.client.entity.MosClientProperties;
import mt.spring.mos.client.entity.dto.MergeFileDto;
import mt.spring.mos.client.entity.dto.Thumb;
import mt.spring.mos.client.service.strategy.WeightStrategy;
import mt.spring.mos.client.utils.FfmpegUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2020/11/21
 */
@Service
@Slf4j
public class ClientService implements InitializingBean {
	@Autowired
	private MosClientProperties mosClientProperties;
	@Autowired
	private WeightStrategy pathStrategy;
	private ThreadPoolExecutor threadPoolExecutor;
	
	private void assertPathnameIsValid(String pathname, String name) {
		Assert.state(StringUtils.isNotBlank(pathname), name + "不能为空");
		Assert.state(!pathname.contains(".."), name + "非法");
	}
	
	public String getAvaliableBasePath(long fileSize) {
		return pathStrategy.getBasePath(mosClientProperties.getDetailBasePaths(), fileSize);
	}
	
	public void upload(InputStream inputStream, String pathname, long size) throws IOException {
		assertPathnameIsValid(pathname, "pathname");
		log.info("上传文件：{}", pathname);
		File desFile = new File(getAvaliableBasePath(size), pathname);
		if (desFile.exists()) {
			log.info("文件已存在，进行覆盖上传");
		}
		File parentFile = desFile.getParentFile();
		if (!parentFile.exists()) {
			log.info("创建路径：{}", parentFile.getPath());
			parentFile.mkdirs();
		}
		log.info("上传至：{}", desFile.getPath());
		try (OutputStream outputStream = new FileOutputStream(desFile)) {
			log.info("进行流拷贝...");
			IOUtils.copyLarge(inputStream, outputStream);
			log.info("{}上传完成!", pathname);
		} finally {
			inputStream.close();
		}
	}
	
	public void deleteFile(String pathname) {
		assertPathnameIsValid(pathname, "pathname");
		File file = getFile(pathname);
		if (file != null && file.exists() && file.isFile()) {
			log.info("删除文件：{}", file.getPath());
			FileUtils.deleteQuietly(file);
		}
	}
	
	public void deleteDir(String path) throws IOException {
		assertPathnameIsValid(path, "path");
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		File file = getFile(path);
		if (file != null && file.exists() && file.isDirectory()) {
			log.info("删除文件夹：{}", file.getPath());
			FileUtils.deleteDirectory(file);
		}
	}
	
	public long getSize(String pathname) {
		for (MosClientProperties.BasePath detailBasePath : mosClientProperties.getDetailBasePaths()) {
			File file = new File(detailBasePath.getPath(), pathname);
			if (file.exists()) {
				return FileUtils.sizeOf(file);
			}
		}
		return -1;
	}
	
	public void moveFile(String srcPathname, String desPathname, boolean cover) {
		assertPathnameIsValid(srcPathname, "srcPathname");
		assertPathnameIsValid(desPathname, "desPathname");
		mosClientProperties.getDetailBasePaths().stream().filter(basePath -> new File(basePath.getPath(), srcPathname).exists()).forEach(basePath -> {
			String path = basePath.getPath();
			File srcFile = new File(path, srcPathname);
			File desFile = new File(path, desPathname);
			if (desFile.exists()) {
				if (cover) {
					desFile.delete();
				} else {
					throw new IllegalStateException("目标文件" + desPathname + "已存在");
				}
			}
			try {
				FileUtils.moveFile(srcFile, desFile);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		//自动创建文件夹
		for (MosClientProperties.BasePath detailBasePath : mosClientProperties.getDetailBasePaths()) {
			File file = new File(detailBasePath.getPath());
			if (!file.exists()) {
				file.mkdirs();
			}
		}
		
		threadPoolExecutor = new ThreadPoolExecutor(mosClientProperties.getMergeThreadPoolCore(), mosClientProperties.getMergeThreadPoolCore(), 0, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
	}
	
	private File getFile(String pathname) {
		return mosClientProperties.getDetailBasePaths().stream().map(basePath -> new File(basePath.getPath(), pathname)).filter(File::exists).findFirst().orElse(null);
	}
	
	public MergeResult mergeFiles(MergeFileDto mergeFileDto) throws IOException {
		log.info("合并：{} -> {}", mergeFileDto.getPath(), mergeFileDto.getDesPathname());
		MergeResult mergeResult = new MergeResult();
		assertPathnameIsValid(mergeFileDto.getPath(), "path");
		String desPathname = mergeFileDto.getDesPathname();
		assertPathnameIsValid(desPathname, "desPathname");
		Assert.notNull(mergeFileDto.getChunks(), "分片数不能为空");
		File path = getFile(mergeFileDto.getPath());
		Assert.state(path != null && path.isDirectory(), "合并路径不是文件夹：" + path);
		long fileSize = 0;
		List<File> srcFiles = new ArrayList<>();
		for (int i = 0; i < mergeFileDto.getChunks(); i++) {
			File srcFile = getFile(mergeFileDto.getPath() + "/part" + i);
			Assert.notNull(srcFile, "文件不存在：" + srcFile);
			Assert.state(srcFile.exists() && srcFile.isFile(), srcFile + "不是文件");
			srcFiles.add(srcFile);
		}
		for (File srcFile : srcFiles) {
			fileSize += srcFile.length();
		}
		String avaliableBasePath = getAvaliableBasePath(fileSize);
		long partSize = srcFiles.get(0).length();
		
		log.info("开始合并文件：{}", desPathname);
		File desFile = new File(avaliableBasePath, desPathname);
		int offset = 0;
		mergeResult.setFile(desFile);
		if (mergeFileDto.isEncode()) {
			try (RandomAccessFile randomAccessFile = new RandomAccessFile(desFile, "rw")) {
				byte[] fileHead = MosFileEncodeUtils.getFileHead(mergeFileDto.getDesPathname());
				randomAccessFile.write(fileHead);
				offset = fileHead.length;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		CountDownLatch countDownLatch = new CountDownLatch(srcFiles.size());
		for (int i = 0; i < srcFiles.size(); i++) {
			threadPoolExecutor.submit(new MergeTask(srcFiles.get(i), desFile, i, partSize, countDownLatch, offset));
		}
		try {
			countDownLatch.await();
			log.info("文件合并完成,合并文件：{}", desPathname);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			FileUtils.deleteDirectory(path);
		}
		mergeResult.setLength(mergeFileDto.isEncode() ? desFile.length() - offset : desFile.length());
		return mergeResult;
	}
	
	@SneakyThrows
	public String md5(String pathname) {
		File file = getFile(pathname);
		Assert.notNull(file, "文件" + pathname + "不存在");
		try (FileInputStream inputStream = new FileInputStream(file)) {
			return DigestUtils.md5Hex(inputStream);
		}
	}
	
	public String moveFileToEncodeAndMd5(File srcFile, String relaParentPath) {
		OutputStream outputStream = null;
		RepeatableBoundedFileInputStream inputStream = null;
		try {
			inputStream = new RepeatableBoundedFileInputStream(new BoundedInputStream(new FileInputStream(srcFile)));
			String md5 = DigestUtils.md5Hex(inputStream);
			String pathname = relaParentPath + "/" + md5;
			inputStream.reset();
			File desFile = new File(srcFile.getParent(), md5);
			if (!desFile.exists()) {
				outputStream = new MosEncodeOutputStream(new FileOutputStream(desFile), pathname);
				IOUtils.copy(inputStream, outputStream);
			}
			return md5;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(inputStream);
			IOUtils.closeQuietly(outputStream);
			srcFile.delete();
		}
	}
	
	public String getParentPath(String pathname) {
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		int lastIndexOf = pathname.lastIndexOf("/");
		String parentPath = pathname.substring(0, lastIndexOf);
		if (StringUtils.isBlank(parentPath)) {
			return "/";
		}
		return parentPath;
	}
	
	/**
	 * 新增截图
	 *
	 * @param pathname 文件路径
	 * @param width    宽度
	 * @param seconds  截图时间
	 * @return 截图
	 */
	public Thumb addThumb(String pathname, Integer width, Integer seconds, String encodeKey) {
		Assert.notNull(pathname, "pathname can not be null");
		Assert.notNull(width, "width can not be null");
		Assert.notNull(seconds, "seconds can not be null");
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		File file = getFile(pathname);
		Assert.notNull(file, "文件" + pathname + "不存在");
		log.info("{}生成{}s截图...", pathname, seconds);
		InputStream inputStream = null;
		FileOutputStream outputStream = null;
		File tempFile = null;
		try {
			inputStream = new FileInputStream(file);
			if (StringUtils.isNotBlank(encodeKey)) {
				inputStream = new MosEncodeInputStream(inputStream, encodeKey);
			}
			tempFile = new File(file.getParent(), UUID.randomUUID().toString());
			log.info("创建临时文件{}", tempFile);
			outputStream = new FileOutputStream(tempFile);
			IOUtils.copy(inputStream, outputStream);
			File tempJpgFile = new File(file.getParent(), UUID.randomUUID().toString());
			log.info("生成截图{} -> {}", tempFile, tempJpgFile);
			FfmpegUtils.screenShot(tempFile, tempJpgFile, width, seconds);
			String parentPath = getParentPath(pathname);
			long size = tempJpgFile.length();
			log.info("移动临时文件");
			String md5 = moveFileToEncodeAndMd5(tempJpgFile, parentPath);
			Thumb thumb = new Thumb();
			thumb.setMd5(md5);
			thumb.setPathname(parentPath + "/" + md5);
			thumb.setSeconds(seconds);
			thumb.setFormat("jpg");
			thumb.setSize(size);
			thumb.setWidth(width);
			return thumb;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(inputStream);
			IOUtils.closeQuietly(outputStream);
			if (tempFile != null) {
				tempFile.delete();
			}
		}
	}
	
	@Data
	static class MergeTask implements Runnable {
		private File srcFile;
		private int chunkIndex;
		private File desFile;
		private long partSize;
		private CountDownLatch countDownLatch;
		private int offset;
		
		public MergeTask(File srcFile, File desFile, int chunkIndex, long partSize, CountDownLatch countDownLatch, int offset) {
			this.srcFile = srcFile;
			this.desFile = desFile;
			this.chunkIndex = chunkIndex;
			this.partSize = partSize;
			this.countDownLatch = countDownLatch;
			this.offset = offset;
		}
		
		@Override
		public void run() {
			RandomAccessFile randomAccessFile = null;
			InputStream in = null;
			try {
				randomAccessFile = new RandomAccessFile(desFile, "rw");
				in = new FileInputStream(srcFile);
				randomAccessFile.seek(offset + chunkIndex * partSize);
				// 每次读取的大小
				byte[] buffer = new byte[4096];
				// 实际读取的大小
				int length;
				while ((length = in.read(buffer)) != -1) {
					randomAccessFile.write(buffer, 0, length);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						log.error(e.getMessage(), e);
					}
				}
				if (randomAccessFile != null) {
					try {
						randomAccessFile.close();
					} catch (IOException e) {
						log.error(e.getMessage(), e);
					}
				}
				countDownLatch.countDown();
			}
		}
	}
}
