package mt.spring.mos.server.service;

import lombok.extern.slf4j.Slf4j;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.common.utils.SpringUtils;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.base.utils.CollectionUtils;
import mt.spring.mos.server.config.AsyncConfiguration;
import mt.spring.mos.server.dao.UploadFileMapper;
import mt.spring.mos.server.entity.dto.InitUploadDto;
import mt.spring.mos.server.entity.dto.MergeFileResult;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.FileHouse;
import mt.spring.mos.server.entity.po.UploadFile;
import mt.spring.mos.server.entity.po.UploadFileItem;
import mt.spring.mos.server.service.clientapi.ClientApiFactory;
import mt.spring.mos.server.service.clientapi.IClientApi;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2023/9/9
 */
@Service
@Slf4j
public class UploadFileService extends BaseServiceImpl<UploadFile> {
	@Autowired
	private UploadFileItemService uploadFileItemService;
	@Autowired
	private FileHouseService fileHouseService;
	@Autowired
	private ClientService clientService;
	@Autowired
	private ClientWorkLogService clientWorkLogService;
	@Autowired
	private ClientApiFactory clientApiFactory;
	@Autowired
	private RedissonClient redissonClient;
	@Autowired
	private AuditService auditService;
	@Autowired
	private TransactionTemplate transactionTemplate;
	@Autowired
	private UploadFileMapper uploadFileMapper;
	
	/**
	 * 初始化上传文件
	 *
	 * @param bucketId  桶id
	 * @param pathname  文件路径
	 * @param totalMd5  文件md5
	 * @param totalSize 文件大小
	 * @param chunks    块数
	 * @return 存在的块
	 */
	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
	public InitUploadDto init(@NotNull Long bucketId, @NotNull String pathname, @NotNull String totalMd5, @NotNull Long totalSize, @NotNull Integer chunks) {
		log.info("初始化上传文件：bucketId={},pathname={},totalMd5={},totalSize={},chunks={}", bucketId, pathname, totalMd5, totalSize, chunks);
		InitUploadDto initUploadDto = new InitUploadDto();
		FileHouse fileHouse = fileHouseService.findOneOkFile(totalMd5, totalSize);
		if (fileHouse != null) {
			log.info("文件已存在:{},fileHouseId={}", pathname, fileHouse.getId());
			initUploadDto.setFileExists(true);
			initUploadDto.setFileHouse(fileHouse);
			return initUploadDto;
		}
		String pathMd5 = getPathMd5(pathname);
		String lockKey = getLockKey(bucketId, pathMd5);
		RLock lock = redissonClient.getLock(lockKey);
		try {
			lock.lock();
			initUploadDto.setFileExists(false);
			UploadFile uploadFile = findOneByBucketAndPathname(bucketId, pathname);
			if (uploadFile == null) {
				Client client = clientService.findRandomAvalibleClientForUpload(totalSize);
				//文件不存在，进行创建
				log.info("文件不存在，进行创建");
				try {
					uploadFile = new UploadFile();
					uploadFile.setBucketId(bucketId);
					uploadFile.setPathMd5(pathMd5);
					uploadFile.setMd5(totalMd5);
					uploadFile.setSizeByte(totalSize);
					uploadFile.setChunks(chunks);
					uploadFile.setClientId(client.getId());
					uploadFile.setClientPath(concatClientPath(totalMd5, bucketId, pathname));
					save(uploadFile);
					return initUploadDto;
				} catch (DuplicateKeyException duplicateKeyException) {
					//重复插入，可能是并发导致的，重新查找
					throw new IllegalStateException("不UploadFileService支持并发上传同一个文件：" + pathname + "，请稍后重试");
				}
			}
			//上传文件已存在，未上传完成
			Long clientId = uploadFile.getClientId();
			Client client = clientService.findById(clientId);
			Long uploadFileId = uploadFile.getId();
			if (client.getStatus() != Client.ClientStatus.UP) {
				//1.client已挂掉，需要重新分配client，删掉原来的文件
				log.info("client已挂掉，需要重新分配client，删掉原来的文件");
				Client newClient = clientService.findRandomAvalibleClientForUpload(totalSize);
				if (!newClient.getId().equals(client.getId())) {
					uploadFile.setClientId(newClient.getId());
					clientWorkLogService.addDeleteDir(client.getId(), lockKey, uploadFile.getClientPath());
					uploadFileItemService.deleteItems(uploadFileId);
					client = newClient;
				}
			}
			//2.文件大小不一致，删掉原来的文件，重新创建
			if (!Objects.equals(totalMd5, uploadFile.getMd5()) || !Objects.equals(chunks, uploadFile.getChunks())) {
				clientApiFactory.getClientApi(client).deleteDir(uploadFile.getClientPath());
				uploadFileItemService.deleteItems(uploadFileId);
			}
			uploadFile.setChunks(chunks);
			uploadFile.setMd5(totalMd5);
			uploadFile.setSizeByte(totalSize);
			//3.查找已上传的块
			List<UploadFileItem> items = uploadFileItemService.findItems(uploadFileId);
			if (CollectionUtils.isNotEmpty(items)) {
				initUploadDto.setExistedChunkIndexs(items.stream().map(UploadFileItem::getChunkIndex).collect(Collectors.toList()));
			}
			
			//清空client的工作日志
			clientWorkLogService.deleteByLockKey(lockKey);
			
			updateById(uploadFile);
			return initUploadDto;
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}
	
	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
	public void upload(long bucketId, @NotNull String pathname, @NotNull String chunkMd5, int chunkIndex, @NotNull InputStream inputStream) throws IOException {
		Assert.notBlank(pathname, "pathname不能为空");
		Assert.notNull(chunkMd5, "chunkMd5不能为空");
		Assert.notNull(inputStream, "上传文件不能为空");
		UploadFile uploadFile = findOneByBucketAndPathname(bucketId, pathname);
		Assert.notNull(uploadFile, "uploadFile不存在，bucketId=" + bucketId + ",pathname=" + pathname);
		String lockKey = getLockKey(bucketId, getPathMd5(pathname));
		RLock lock = redissonClient.getLock(lockKey);
		long start = System.currentTimeMillis();
		try {
			lock.lock();
			log.info("上传文件分片，bucketId={},pathname={},chunkIndex={}", bucketId, pathname, chunkIndex);
			Client client = clientService.findById(uploadFile.getClientId());
			Assert.state(client.getStatus() == Client.ClientStatus.UP, "存储服务器不可用");
			Long uploadFileId = uploadFile.getId();
			UploadFileItem uploadFileItem = uploadFileItemService.findItem(uploadFileId, chunkIndex);
			if (uploadFileItem != null) {
				//已存在，文件覆盖
				log.info("文件分片已存在，文件覆盖,chunkIndex:{}", chunkIndex);
				uploadFileItemService.deleteById(uploadFileItem);
			}
			int chunkSize = inputStream.available();
			IClientApi clientApi = clientApiFactory.getClientApi(client);
			String itemClientPath = getItemName(uploadFile.getClientPath(), chunkIndex);
			//上传
			clientApi.upload(inputStream, itemClientPath);
			auditService.writeBytesRecord(bucketId, chunkSize);
			uploadFileItem = new UploadFileItem();
			uploadFileItem.setUploadFileId(uploadFileId);
			uploadFileItem.setClientPath(itemClientPath);
			uploadFileItem.setChunkIndex(chunkIndex);
			uploadFileItem.setSizeByte((long) chunkSize);
			uploadFileItem.setMd5(chunkMd5);
			uploadFileItemService.save(uploadFileItem);
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
			log.info("fileHouseId upload用时：{}ms", System.currentTimeMillis() - start);
		}
	}
	
	@Async(AsyncConfiguration.DEFAULT_EXECUTOR_NAME)
	public Future<FileHouse> mergeFiles(@NotNull Long bucketId, @NotNull String pathname, boolean updateMd5, FileHouseService.MergeDoneCallback mergeDoneCallback) {
		long start = System.currentTimeMillis();
		try {
			FileHouse result = transactionTemplate.execute(status -> merge(bucketId, pathname, updateMd5));
//			FileHouse result = merge(bucketId, pathname, updateMd5);
			if (mergeDoneCallback != null) {
				mergeDoneCallback.callback(result);
			}
			return new AsyncResult<>(result);
		} finally {
			long cost = System.currentTimeMillis() - start;
			log.info("合并完成，bucketId={},pathname={},用时{}ms", bucketId, pathname, cost);
		}
	}
	
	private FileHouse merge(@NotNull Long bucketId, @NotNull String pathname, boolean updateMd5) {
		String pathMd5 = getPathMd5(pathname);
		log.info("开始合并文件，bucketId={},pathname={},pathMd5={}", bucketId, pathname, pathMd5);
		UploadFile uploadFile = findOneByBucketAndPathname(bucketId, pathname);
		Assert.notNull(uploadFile, "uploadFile不存在");
		log.info("合并路径：{},pathMd5={}", uploadFile.getClientPath(), pathMd5);
		Long uploadFileId = uploadFile.getId();
		String lockKey = getLockKey(bucketId, getPathMd5(pathname));
		RLock lock = redissonClient.getLock(lockKey);
		try {
			lock.lock();
			for (; ; ) {
				String md5 = uploadFile.getMd5();
				Long sizeByte = uploadFile.getSizeByte();
				//1把
				FileHouse fileHouse = fileHouseService.findByMd5AndSize(md5, sizeByte);
				if (fileHouse != null) {
					if (fileHouse.getFileStatus() == FileHouse.FileStatus.OK) {
						log.info("fileHouse文件已存在，跳过合并：{},md5={}，pathMd5={}", pathname, md5, pathMd5);
						clearUploadFile(uploadFileId);
						return fileHouse;
					} else {
						//临时文件，直接删除
						fileHouseService.clearFileHouse(fileHouse, false);
					}
				}
				//新增fileHouse，2把
				log.info("createFileHouse,pathMd5={}", pathMd5);
				fileHouse = fileHouseService.createFileHouse(md5, sizeByte, uploadFile.getClientId());
				if (fileHouse == null) {
					//竞争失败，重新抢
					continue;
				}
				//校验文件完整性
				int chunks = uploadFileItemService.countItems(uploadFileId);
				Assert.state(uploadFile.getChunks() == chunks, "合并失败,文件" + pathname + "还未上传完整，分片数：" + uploadFile.getChunks() + "，已上传分片数：" + chunks);
				
				//合并
				Client client = clientService.findById(uploadFile.getClientId());
				Assert.notNull(client, "client不存在" + uploadFile.getClientId());
				Assert.state(client.getStatus() == Client.ClientStatus.UP, "存储服务器不可用:" + client.getName());
				IClientApi clientApi = clientApiFactory.getClientApi(client);
				String fileHousePathname = fileHouseService.getUploadPathname(md5);
				log.info("合并路径：{}", uploadFile.getClientPath());
				MergeFileResult mergeFileResult = clientApi.mergeFiles(uploadFile.getClientPath(), uploadFile.getChunks(), fileHousePathname, updateMd5, true);
				long length = mergeFileResult.getLength();
				String totalMd5 = StringUtils.isNotBlank(mergeFileResult.getMd5()) ? mergeFileResult.getMd5() : md5;
				if (updateMd5) {
					if (totalMd5 == null) {
						totalMd5 = clientApi.md5(pathname);
					}
					log.info("更新的md5：{}，length:{}", totalMd5, length);
					FileHouse findFileHouse = fileHouseService.findByMd5AndSize(totalMd5, length);
					if (findFileHouse != null && findFileHouse.getFileStatus() == FileHouse.FileStatus.OK) {
						log.info("已存在相同的文件，删除当前文件");
						clearUploadFile(uploadFileId);
						fileHouseService.clearFileHouse(fileHouse, false);
						return findFileHouse;
					}
				}
				
				fileHouse.setFileStatus(FileHouse.FileStatus.OK);
				fileHouse.setMd5(totalMd5);
				fileHouse.setSizeByte(length);
				fileHouse.setPathname(fileHousePathname);
				fileHouse.setEncode(true);
				fileHouse.setDataFragmentsCount(1);
				fileHouse.setBackFails(0);
				fileHouse.setChunks(1);
				fileHouseService.updateById(fileHouse);
				clearUploadFile(uploadFileId);
				log.info("文件合并完成：{}", pathname);
				return fileHouse;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw e;
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void clearUploadFile(Long uploadFileId) {
		UploadFile uploadFile = findById(uploadFileId);
		Assert.notNull(uploadFile, "uploadFile不存在");
		
		//清空上传文件
		uploadFileItemService.deleteItems(uploadFileId);
		deleteById(uploadFileId);
		//清空客户端文件
		clientWorkLogService.addDeleteDir(uploadFile.getClientId(), getLockKey(uploadFile.getBucketId(), uploadFile.getPathMd5()), uploadFile.getClientPath());
	}
	
	public FileHouse uploadLocalFile(@NotNull Long bucketId, @NotNull File file) throws Exception {
		UploadFileService uploadFileService = SpringUtils.getBean(UploadFileService.class);
		Path path = file.toPath();
		try (InputStream md5InputStream = Files.newInputStream(path);
			 InputStream inputStream = Files.newInputStream(path)) {
			String md5 = DigestUtils.md5Hex(md5InputStream);
			String pathname = "/mos-local/" + file.getName();
			InitUploadDto init = uploadFileService.init(bucketId, pathname, md5, file.length(), 1);
			if (init.isFileExists()) {
				return init.getFileHouse();
			}
			uploadFileService.upload(bucketId, pathname, md5, 0, inputStream);
			Future<FileHouse> fileHouseFuture = uploadFileService.mergeFiles(bucketId, pathname, false, null);
			return fileHouseFuture.get();
		}
	}
	
	private String getItemName(String clientPath, int index) {
		return clientPath + "/part" + index;
	}
	
	private String getLockKey(Long bucketId, String pathMd5) {
		return "uploadFile-" + bucketId + "-" + pathMd5;
	}
	
	private String concatClientPath(String md5, Long bucketId, String pathname) {
		return "/upload/" + StringUtils.join(Arrays.asList(bucketId, md5, getPathMd5(pathname)), "-");
	}
	
	public UploadFile findOneByBucketAndPathname(@NotNull Long bucketId, @NotNull String pathname) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("bucketId", Filter.Operator.eq, bucketId));
		filters.add(new Filter("pathMd5", Filter.Operator.eq, getPathMd5(pathname)));
		return findOneByFilters(filters);
	}
	
	private String getPathMd5(String pathname) {
		return DigestUtils.md5Hex(formatPathname(pathname));
	}
	
	private String formatPathname(String path) {
		if (!"/".equals(path) && path.endsWith("/")) {
			//去掉末尾的/
			path = path.substring(0, path.length() - 1);
		}
		if (!path.startsWith("/")) {
			//加上开头的/
			path = "/" + path;
		}
		return path;
	}
	
	public List<UploadFile> findNotUsedFileHouseList(int days) {
		return uploadFileMapper.findNotUsedFileHouseList(days + " 0:0:0");
	}
}
