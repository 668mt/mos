package mt.spring.mos.client.service;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.client.entity.MosClientProperties;
import mt.spring.mos.client.entity.dto.MergeFileDto;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author Martin
 * @Date 2020/11/21
 */
@Service
@Slf4j
public class ClientService implements InitializingBean {
	@Autowired
	private MosClientProperties mosClientProperties;
	
	private void assertPathnameIsValid(String pathname, String name) {
		Assert.state(StringUtils.isNotBlank(pathname), name + "不能为空");
		Assert.state(!pathname.contains(".."), name + "非法");
	}
	
	public String getAvaliableBasePath(long fileSize) {
		String[] basePaths = mosClientProperties.getBasePaths();
		Assert.notNull(basePaths, "未配置basePath");
		List<File> collect = Arrays.stream(basePaths).map(File::new).filter(file1 -> {
			//空闲空间占比
			long freeSpace = file1.getFreeSpace();
			return freeSpace > fileSize && BigDecimal.valueOf(freeSpace).compareTo(mosClientProperties.getMinAvaliableSpaceGB().multiply(BigDecimal.valueOf(FileUtils.ONE_GB))) > 0;
		}).collect(Collectors.toList());
		Assert.notEmpty(collect, "无可用存储空间使用");
		return collect.get(new Random().nextInt(collect.size())).getPath();
	}
	
	public void upload(InputStream inputStream, String pathname, long size) throws IOException {
		assertPathnameIsValid(pathname, "pathname");
		log.info("上传文件：{}", pathname);
		String[] basePaths = mosClientProperties.getBasePaths();
		Assert.notNull(basePaths, "未配置basePath");
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
		String[] basePaths = mosClientProperties.getBasePaths();
		if (basePaths == null) {
			return -1;
		}
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		for (String basePath : basePaths) {
			File file = new File(basePath + pathname);
			if (file.exists()) {
				return FileUtils.sizeOf(file);
			}
		}
		return -1;
	}
	
	public void moveFile(String srcPathname, String desPathname, boolean cover) {
		assertPathnameIsValid(srcPathname, "srcPathname");
		assertPathnameIsValid(desPathname, "desPathname");
		Stream.of(mosClientProperties.getBasePaths())
				.filter(s -> new File(s, srcPathname).exists())
				.forEach(basePath -> {
					File srcFile = new File(basePath, srcPathname);
					File desFile = new File(basePath, desPathname);
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
	
	private ThreadPoolExecutor threadPoolExecutor;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		//自动创建文件夹
		String[] basePaths = mosClientProperties.getBasePaths();
		if (basePaths != null) {
			for (String basePath : basePaths) {
				File file = new File(basePath);
				if (!file.exists()) {
					file.mkdirs();
				}
			}
		}
		
		threadPoolExecutor = new ThreadPoolExecutor(mosClientProperties.getMergeThreadPoolCore(), mosClientProperties.getMergeThreadPoolCore(), 0, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
	}
	
	private File getFile(String pathname) {
		Assert.notNull(mosClientProperties.getBasePaths(), "未配置basePaths");
		for (String basePath : mosClientProperties.getBasePaths()) {
			File file = new File(basePath, pathname);
			if (file.exists()) {
				return file;
			}
		}
		return null;
	}
	
	public File mergeFiles(MergeFileDto mergeFileDto) throws IOException {
		log.info("合并：{} -> {}", mergeFileDto.getPath(), mergeFileDto.getDesPathname());
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
		CountDownLatch countDownLatch = new CountDownLatch(srcFiles.size());
		for (int i = 0; i < srcFiles.size(); i++) {
			threadPoolExecutor.submit(new MergeTask(srcFiles.get(i), desFile, i, partSize, countDownLatch));
		}
		try {
			countDownLatch.await();
			log.info("文件合并完成,合并文件：{}", desPathname);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			FileUtils.deleteDirectory(path);
		}
		return desFile;
	}
	
	@SneakyThrows
	public String md5(String pathname) {
		File file = getFile(pathname);
		Assert.notNull(file, "文件" + pathname + "不存在");
		try (FileInputStream inputStream = new FileInputStream(file)) {
			return DigestUtils.md5Hex(inputStream);
		}
	}
	
	@Data
	static class MergeTask implements Runnable {
		private File srcFile;
		private int chunkIndex;
		private File desFile;
		private long partSize;
		private CountDownLatch countDownLatch;
		
		public MergeTask(File srcFile, File desFile, int chunkIndex, long partSize, CountDownLatch countDownLatch) {
			this.srcFile = srcFile;
			this.desFile = desFile;
			this.chunkIndex = chunkIndex;
			this.partSize = partSize;
			this.countDownLatch = countDownLatch;
		}
		
		@Override
		public void run() {
			RandomAccessFile randomAccessFile = null;
			InputStream in = null;
			try {
				randomAccessFile = new RandomAccessFile(desFile, "rw");
				in = new FileInputStream(srcFile);
				randomAccessFile.seek(chunkIndex * partSize);
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
