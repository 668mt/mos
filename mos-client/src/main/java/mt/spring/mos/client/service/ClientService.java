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
import mt.spring.mos.client.entity.dto.IsExistsDTO;
import mt.spring.mos.client.entity.dto.MergeFileDto;
import mt.spring.mos.client.service.strategy.PathStrategy;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	private PathStrategy pathStrategy;
	private ThreadPoolExecutor threadPoolExecutor;
	
	private void assertPathnameIsValid(String pathname, String name) {
		Assert.state(StringUtils.isNotBlank(pathname), name + "不能为空");
		Assert.state(!pathname.contains(".."), name + "非法");
	}
	
	public String getAvaliableBasePath(long fileSize) {
		return pathStrategy.getBasePath(fileSize);
	}
	
	public String getAvaliableBasePath(long fileSize, String pathname) {
		return pathStrategy.getBasePath(fileSize, pathname);
	}
	
	public void upload(InputStream inputStream, String pathname, long size, boolean cover) throws IOException {
		assertPathnameIsValid(pathname, "pathname");
		log.info("上传文件：{}", pathname);
		File desFile = new File(getAvaliableBasePath(size, pathname), pathname);
		if (desFile.exists()) {
			if (cover) {
				log.info("文件{}已存在，进行覆盖上传", pathname);
			} else {
				log.info("文件{}已存在，跳过上次", pathname);
				IOUtils.closeQuietly(inputStream);
				return;
			}
		}
		File parentFile = desFile.getParentFile();
		if (!parentFile.exists()) {
			log.info("创建路径：{}", parentFile.getPath());
			parentFile.mkdirs();
		}
		log.info("上传至：{}", desFile.getPath());
		try (OutputStream outputStream = Files.newOutputStream(desFile.toPath())) {
			IOUtils.copyLarge(inputStream, outputStream);
			Assert.state(desFile.exists(), "文件上传失败，目标文件不存在：" + desFile);
			log.info("{}上传完成!", pathname);
		} finally {
			inputStream.close();
		}
	}
	
	public void deleteFile(String pathname) {
		assertPathnameIsValid(pathname, "pathname");
		log.info("删除文件：{}", pathname);
		File file = getFile(pathname);
		if (file != null && file.exists() && file.isFile()) {
			log.info("删除本地文件：{}", file.getPath());
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
	
	public MergeResult mergeFiles(MergeFileDto mergeFileDto) throws Exception {
		log.info("合并：{} -> {}", mergeFileDto.getPath(), mergeFileDto.getDesPathname());
		MergeResult mergeResult = new MergeResult();
		assertPathnameIsValid(mergeFileDto.getPath(), "path");
		String desPathname = mergeFileDto.getDesPathname();
		assertPathnameIsValid(desPathname, "desPathname");
		Assert.notNull(mergeFileDto.getChunks(), "分片数不能为空");
		//创建文件夹
//		for (MosClientProperties.BasePath detailBasePath : mosClientProperties.getDetailBasePaths()) {
//			File file = new File(detailBasePath.getPath(), desPathname);
//			if (file.exists()) {
//				file.delete();
//			}
//			file.getParentFile().mkdirs();
//		}
		
		//获取存在的文件路径
		File path = getFile(mergeFileDto.getPath());
		File desFile = getFile(desPathname);
		//文件头
		byte[] fileHead = MosFileEncodeUtils.getFileHead(mergeFileDto.getDesPathname());
		int offset = fileHead.length;
		if (desFile != null && desFile.isFile()) {
			//已经合并过
			log.info("已经合并过该文件:{}", desFile);
			FileUtils.deleteQuietly(desFile);
		}
		//未合并
		Assert.state(path != null && path.isDirectory(), "合并路径不存在或不是文件夹：" + mergeFileDto.getPath());
		long fileSize = 0;
		List<File> srcFiles = new ArrayList<>();
		for (int i = 0; i < mergeFileDto.getChunks(); i++) {
			String p = mergeFileDto.getPath() + "/part" + i;
			File srcFile = getFile(p);
			Assert.notNull(srcFile, "文件不存在：" + p);
			Assert.state(srcFile.exists() && srcFile.isFile(), srcFile + "不是文件");
			srcFiles.add(srcFile);
		}
		for (File srcFile : srcFiles) {
			fileSize += srcFile.length();
		}
		String avaliableBasePath = getAvaliableBasePath(fileSize);
		long partSize = srcFiles.get(0).length();
		
		log.info("开始合并文件：{}", desPathname);
		if (desFile == null) {
			desFile = new File(avaliableBasePath, desPathname);
		}
		mergeResult.setFile(desFile);
		if (mergeFileDto.isEncode()) {
			try (RandomAccessFile randomAccessFile = new RandomAccessFile(desFile, "rw")) {
				randomAccessFile.write(fileHead);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				throw new RuntimeException(e);
			}
		}
		log.info("合并文件：{} -> {}", srcFiles, desFile);
		CountDownLatch countDownLatch = new CountDownLatch(srcFiles.size());
		for (int i = 0; i < srcFiles.size(); i++) {
			threadPoolExecutor.submit(new MergeTask(srcFiles.get(i), desFile, i, partSize, countDownLatch, offset));
		}
		try {
			countDownLatch.await();
			log.info("文件合并完成,合并文件：{}", desPathname);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		//删除文件夹
		log.info("删除文件夹：{}", path);
		FileUtils.deleteDirectory(path);
		
		mergeResult.setLength(mergeFileDto.isEncode() ? desFile.length() - offset : desFile.length());
		log.info("合并后的文件大小：{}", mergeResult.getLength());
		return mergeResult;
	}
	
	@SneakyThrows
	public String md5(String pathname) {
		File file = getFile(pathname);
		Assert.notNull(file, "文件" + pathname + "不存在");
		try (InputStream inputStream = new MosEncodeInputStream(new FileInputStream(file), pathname)) {
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
	
	public Map<String, Boolean> isExists(@NotNull IsExistsDTO isExistsDTO) {
		Map<String, Boolean> result = new HashMap<>(16);
		List<String> list = isExistsDTO.getPathname();
		if (list != null) {
			for (String s : list) {
				result.put(s, getFile(s) != null);
			}
		}
		return result;
	}
	
	/**
	 * 是否健康
	 *
	 * @return 是否健康
	 */
	public boolean isHealth() {
		List<MosClientProperties.BasePath> detailBasePaths = mosClientProperties.getDetailBasePaths();
		return detailBasePaths.stream().allMatch(basePath -> new File(basePath.getPath()).exists());
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
