package mt.spring.mos.server.service;

import lombok.extern.slf4j.Slf4j;
import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.base.stream.LimitInputStream;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.base.utils.IOUtils;
import mt.spring.mos.base.utils.SizeUtils;
import mt.spring.mos.base.utils.SpeedUtils;
import mt.spring.mos.server.dao.FileHouseMapper;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.FileHouse;
import mt.spring.mos.server.entity.po.FileHouseRelaClient;
import mt.spring.mos.server.entity.vo.BackVo;
import mt.spring.mos.server.service.clientapi.ClientApiFactory;
import mt.spring.mos.server.service.clientapi.IClientApi;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static mt.common.tkmapper.Filter.Operator.eq;

/**
 * @Author Martin
 * @Date 2020/11/21
 */
@Service
@Slf4j
public class FileHouseService extends BaseServiceImpl<FileHouse> {
	@Autowired
	private FileHouseMapper fileHouseMapper;
	@Autowired
	private ClientService clientService;
	@Autowired
	@Lazy
	private FileHouseItemService fileHouseItemService;
	@Autowired
	@Lazy
	private ResourceService resourceService;
	@Autowired
	@Lazy
	private FileHouseRelaClientService fileHouseRelaClientService;
	@Autowired
	private MosServerProperties mosServerProperties;
	@Autowired
	@Qualifier("backRestTemplate")
	private RestTemplate backRestTemplate;
	@Autowired
	private ClientApiFactory clientApiFactory;
	@Autowired
	private FileHouseLockService fileHouseLockService;
	@Autowired
	private TransactionTemplate transactionTemplate;
	@Autowired
	@Lazy
	private ClientWorkLogService clientWorkLogService;
	
	@Override
	public BaseMapper<FileHouse> getBaseMapper() {
		return fileHouseMapper;
	}
	
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public FileHouse findByMd5AndSize(String md5, long size) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("md5", Filter.Operator.eq, md5));
		filters.add(new Filter("sizeByte", Filter.Operator.eq, size));
		return findOneByFilters(filters);
	}
	
	public FileHouse findOneOkFile(@NotNull String md5, long size) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("md5", Filter.Operator.eq, md5));
		filters.add(new Filter("sizeByte", Filter.Operator.eq, size));
		filters.add(new Filter("fileStatus", Filter.Operator.eq, FileHouse.FileStatus.OK));
		return findOneByFilters(filters);
	}
	
	public String getUploadPathname(String md5) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMM");
		String path = dateFormat.format(new Date());
		return "/" + path + "/" + md5;
	}
	
	@Transactional(rollbackFor = Exception.class)
	public FileHouse createFileHouse(@NotNull String md5, long size, @NotNull Long clientId) {
		Client client = clientService.findRandomAvalibleClientForUpload(size);
		Assert.notNull(client, "无可用的存储服务器");
		try {
			FileHouse fileHouse = new FileHouse();
			fileHouse.setMd5(md5);
			fileHouse.setSizeByte(size);
			fileHouse.setChunks(1);
			fileHouse.setFileStatus(FileHouse.FileStatus.OK);
			fileHouse.setPathname(getUploadPathname(md5));
			fileHouse.setDataFragmentsCount(1);
			save(fileHouse);
			
			fileHouseRelaClientService.bind(fileHouse.getId(), clientId);
			return fileHouse;
		} catch (DuplicateKeyException duplicateKeyException) {
			log.error("md5已存在:{}", md5);
			return null;
		}
	}
	
	@Transactional(propagation = Propagation.MANDATORY)
	public FileHouse findByMd5AndSizeCurrentRead(String md5, long size) {
//		List<Filter> filters = new ArrayList<>();
//		filters.add(new Filter("md5", Filter.Operator.eq, md5));
//		filters.add(new Filter("sizeByte", Filter.Operator.eq, size));
		return fileHouseMapper.findByMd5AndSizeCurrentRead(md5, size);
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void clearFileHouse(@NotNull Long fileHouseId) {
		log.info("clearFileHouse：{}", fileHouseId);
		FileHouse lockedFileHouse = findOneByFilter(new Filter("id", eq, fileHouseId), true);
		if (lockedFileHouse == null) {
			return;
		}
		int countInUsed = resourceService.count(Collections.singletonList(new Filter("fileHouseId", eq, fileHouseId)));
		int thumbCountInUsed = resourceService.count(Collections.singletonList(new Filter("thumbFileHouseId", eq, fileHouseId)));
		if (countInUsed > 0 || thumbCountInUsed > 0) {
			log.info("fileHouse还在被使用，不能清除:{}", lockedFileHouse.getId());
			return;
		}
		List<FileHouseRelaClient> listByFileHouseId = fileHouseRelaClientService.findListByFileHouseId(lockedFileHouse.getId());
		if (CollectionUtils.isNotEmpty(listByFileHouseId)) {
			listByFileHouseId.forEach(fileHouseRelaClient -> {
				String lockKey = getUploadPathname(lockedFileHouse.getMd5());
				clientWorkLogService.addDeleteFile(fileHouseRelaClient.getClientId(), lockKey, lockedFileHouse.getPathname());
				clientWorkLogService.addDeleteDir(fileHouseRelaClient.getClientId(), lockKey, lockedFileHouse.getChunkTempPath());
				fileHouseRelaClientService.deleteById(fileHouseRelaClient);
			});
		}
		deleteById(lockedFileHouse);
	}
	
	public interface MergeDoneCallback {
		void callback(FileHouse fileHouse);
	}
	
	@Autowired
	private RedissonClient redissonClient;

//	/**
//	 * 初始化文件
//	 *
//	 * @param fileHouse     文件
//	 * @param totalMd5      文件md5
//	 * @param totalSize     文件大小
//	 * @param chunks        分片数
//	 * @param pathname      文件路径
//	 * @param initUploadDto 入参
//	 * @return 是否初始化成功
//	 */
//	@Transactional(rollbackFor = Exception.class)
//	public boolean initFileHouse(FileHouse fileHouse, String totalMd5, Long totalSize, Integer chunks, String pathname, InitUploadDto initUploadDto) {
//		long start = System.currentTimeMillis();
//		//文件上传路径
//		String lockKey = getUploadPathname(totalMd5);
//		RLock lock = redissonClient.getLock(lockKey);
//		try {
//			//上锁，避免文件夹并发操作
//			lock.lock();
//			if (fileHouse == null) {
//				fileHouse = createFileHouse(totalMd5, totalSize, null);
//				if (fileHouse != null) {
//					//清空作业
//					clientWorkLogService.deleteByLockKey(lockKey);
//					return true;
//				} else {
//					return false;
//				}
//			}
//			long fileHouseId = fileHouse.getId();
//			//锁定
//			FileHouse findFileHouse = findOneByFilter(new Filter("id", eq, fileHouseId), true);
//			Assert.notNull(findFileHouse, "fileHouse不存在:" + fileHouseId);
//			if (findFileHouse.getFileStatus() == FileHouse.FileStatus.OK) {
//				//未初始化
//				return false;
//			}
//			//还未上传完成
//			FileHouseRelaClient fileHouseRelaClient = fileHouseRelaClientService.findUniqueFileHouseRelaClient(findFileHouse.getId());
//			//原客户端
//			Client client = clientService.findById(fileHouseRelaClient.getClientId());
//			if (!clientService.isAlive(client)) {
//				//原client不可用了，删掉原来的分片，重新找一个可用的client
//				Client newClient = clientService.findRandomAvalibleClientForUpload(totalSize);
//				log.info("{}:原client[{}]不可用，重新分配新的client[{}]", pathname, client.getName(), newClient.getName());
//				//删掉原来上传的分片
//				fileHouseItemService.deleteByFileHouseId(findFileHouse.getId());
//				//新增删除原分片的任务
//				clientWorkLogService.addDeleteDir(client.getId(), lockKey, findFileHouse.getChunkTempPath());
//				//设置新的client
//				fileHouseRelaClient.setClientId(newClient.getId());
//				fileHouseRelaClientService.updateById(fileHouseRelaClient);
//			} else {
//				List<FileHouseItem> items = fileHouseItemService.findList("fileHouseId", findFileHouse.getId());
//				if (CollectionUtils.isEmpty(items)) {
//					return true;
//				}
//
//				if (!Objects.equals(findFileHouse.getChunks(), chunks) || !Objects.equals(items.size(), chunks)) {
//					//分片有变更，删除原有分片
//					fileHouseItemService.deleteByFileHouseId(fileHouseId);
//					clientApiFactory.getClientApi(client).deleteDir(findFileHouse.getChunkTempPath());
//					findFileHouse.setChunks(chunks);
//					updateByIdSelective(findFileHouse);
//					return true;
//				}
//
//				//查找已经上传过的分片
//				Map<String, Integer> map = new HashMap<>();
//				for (FileHouseItem item : items) {
//					String itemPathname = fileHouseItemService.getItemName(findFileHouse, item.getChunkIndex());
//					map.put(itemPathname, item.getChunkIndex());
//				}
//				List<String> pathnames = new ArrayList<>(map.keySet());
//				Map<String, Boolean> result = clientApiFactory.getClientApi(client).isExists(pathnames);
//				List<Integer> existsChunkIndex = new ArrayList<>();
//				for (Map.Entry<String, Boolean> stringBooleanEntry : result.entrySet()) {
//					if (stringBooleanEntry.getValue()) {
//						existsChunkIndex.add(map.get(stringBooleanEntry.getKey()));
//					}
//				}
//				initUploadDto.setExistedChunkIndexs(existsChunkIndex);
//				List<Long> deleteItemIds = items.stream().filter(fileHouseItem -> !existsChunkIndex.contains(fileHouseItem.getChunkIndex())).map(FileHouseItem::getId).collect(Collectors.toList());
//				if (CollectionUtils.isNotEmpty(deleteItemIds)) {
//					fileHouseItemService.deleteByFilter(new Filter("id", in, deleteItemIds));
//				}
//				//清空作业
//				clientWorkLogService.deleteByLockKey(lockKey);
//			}
//			return true;
//		} finally {
//			if (lock.isHeldByCurrentThread()) {
//				lock.unlock();
//			}
//			log.info("initFileHouse用时：{}ms", System.currentTimeMillis() - start);
//		}
//	}

//	@Async
//	public Future<FileHouse> mergeFiles(Long fileHouseId, Integer updateChunks, boolean updateMd5, MergeDoneCallback mergeDoneCallback) {
//		long start = System.currentTimeMillis();
//		try {
//			FileHouse result = merge(fileHouseId, updateChunks, updateMd5);
//			if (result != null && mergeDoneCallback != null) {
//				mergeDoneCallback.callback(result);
//			}
//			return new AsyncResult<>(result);
//		} finally {
//			long cost = System.currentTimeMillis() - start;
//			log.info("fileHouse{}合并完成：{}ms", fileHouseId, cost);
//		}
//	}

//	private FileHouse merge(Long fileHouseId, Integer updateChunks, boolean updateMd5) {
//		FileHouse fileHouse = findOneByFilter(new Filter("id", eq, fileHouseId));
//		Assert.notNull(fileHouse, "fileHouse不能为空");
//		if (fileHouse.getFileStatus() == FileHouse.FileStatus.OK) {
//			return fileHouse;
//		}
//		String lockKey = getUploadPathname(fileHouse.getMd5());
//		if (updateChunks != null) {
//			fileHouse.setChunks(updateChunks);
//		}
//		String pathname = fileHouse.getPathname();
//		log.info("开始合并文件：{}", pathname);
//		RLock mergeLock = redissonClient.getLock("fileHouse-" + fileHouseId);
//		RLock clientFileLock = redissonClient.getLock(lockKey);
//		RLock multiLock = redissonClient.getMultiLock(mergeLock, clientFileLock);
//		try {
//			multiLock.lock();
//			if (fileHouse.getFileStatus() == FileHouse.FileStatus.OK) {
//				return fileHouse;
//			}
//			int chunks = fileHouseItemService.countItems(fileHouse.getId());
//			Assert.state(fileHouse.getChunks() == chunks, "合并失败,文件" + pathname + "还未上传完整，分片数：" + fileHouse.getChunks() + "，已上传分片数：" + chunks);
//			List<FileHouseRelaClient> fileHouseRelaClients = fileHouseRelaClientService.findListByFileHouseId(fileHouse.getId());
//			Assert.state(fileHouseRelaClients.size() == 1, "资源服务器异常，当前资源：" + fileHouseRelaClients.size());
//			FileHouseRelaClient fileHouseRelaClient = fileHouseRelaClients.get(0);
//			//合并
//			Client client = clientService.findById(fileHouseRelaClient.getClientId());
//			Assert.state(clientService.isAlive(client), "存储服务器不可用");
//			IClientApi clientApi = clientApiFactory.getClientApi(client);
//			MergeFileResult mergeFileResult = clientApi.mergeFiles(fileHouse.getChunkTempPath(), fileHouse.getChunks(), pathname, updateMd5, true);
//			long length = mergeFileResult.getLength();
//			String totalMd5 = mergeFileResult.getMd5();
//			fileHouse.setEncode(true);
//			fileHouse.setSizeByte(length);
//			fileHouseItemService.deleteByFilters(Collections.singletonList(new Filter("fileHouseId", eq, fileHouse.getId())));
//			if (updateMd5) {
//				if (totalMd5 == null) {
//					totalMd5 = clientApi.md5(pathname);
//				}
//				log.info("更新的md5：{}，length:{}", totalMd5, length);
//				FileHouse findFileHouse = findByMd5AndSize(totalMd5, length);
//				if (findFileHouse != null && !findFileHouse.getId().equals(fileHouse.getId()) && findFileHouse.getFileStatus() == FileHouse.FileStatus.OK) {
//					log.info("已存在相同的文件，删除此文件");
//					clearFileHouse(fileHouse, false);
//					return fileHouse;
//				} else {
//					fileHouse.setMd5(totalMd5);
//				}
//			}
//			fileHouse.setFileStatus(FileHouse.FileStatus.OK);
//			updateById(fileHouse);
//			log.info("文件合并完成：{}", pathname);
//			return fileHouse;
//		} catch (Exception e) {
//			log.error(e.getMessage(), e);
//			throw e;
//		} finally {
//			multiLock.unlock();
//		}
//	}
	
	public List<FileHouse> findNotUsedFileHouseList(int beforeDays) {
		return fileHouseMapper.findNotUsedFileHouseList(beforeDays + " 0:0:0");
	}
	
	@Autowired
	@Lazy
	private BucketService bucketService;
	
	/**
	 * 查询需要备份的数据，当前数据小于数据分片数
	 *
	 * @return
	 */
	public List<BackVo> findNeedBackFileHouses(int limit) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("status", Filter.Operator.eq, Client.ClientStatus.UP));
		//查询存活的服务
		int aliveCount = clientService.count(filters);
		List<Bucket> buckets = bucketService.findByFilter(new Filter("dataFragmentsAmount", Filter.Operator.gt, 1));
		if (CollectionUtils.isEmpty(buckets)) {
			return Collections.emptyList();
		}
		List<BackVo> list = new ArrayList<>();
		for (Bucket bucket : buckets) {
			//备份数不能大于存活数
			List<BackVo> needBackFileHouseIds = fileHouseMapper.findNeedBackFileHouseIds(bucket.getId(), aliveCount, limit);
			List<BackVo> needBackThumbFileHouseIds = fileHouseMapper.findNeedBackThumbFileHouseIds(bucket.getId(), aliveCount, limit);
			if (CollectionUtils.isNotEmpty(needBackFileHouseIds)) {
				list.addAll(needBackFileHouseIds);
			}
			if (CollectionUtils.isNotEmpty(needBackThumbFileHouseIds)) {
				list.addAll(needBackThumbFileHouseIds);
			}
		}
		return list;
	}
	
	/**
	 * 备份资源
	 *
	 * @param backVo
	 */
	@Transactional(rollbackFor = {Exception.class})
	public void backFileHouse(@NotNull BackVo backVo) {
		Long fileHouseId = backVo.getFileHouseId();
		FileHouse fileHouse = findOneByFilter(new Filter("id", eq, fileHouseId), true);
		if (fileHouse == null) {
			return;
		}
		if (fileHouse.getBackFails() != null && fileHouse.getBackFails() >= 3) {
			return;
		}
		log.info("开始备份fileHouseId：{}", fileHouseId);
		//目标要备份的分片数
		Integer dataFragmentsAmount = backVo.getDataFragmentsAmount();
		List<Client> clients = clientService.findAvaliableClients();
		Assert.notEmpty(clients, "无可用资源服务器");
		List<FileHouseRelaClient> relas = fileHouseRelaClientService.findListByFileHouseId(fileHouseId);
		Assert.notEmpty(relas, "资源不存在");
		if (fileHouse.getDataFragmentsCount() != relas.size()) {
			fileHouse.setDataFragmentsCount(relas.size());
			updateById(fileHouse);
		}
		if (relas.size() >= dataFragmentsAmount) {
			//已经达到数据分片数量了，不需要再进行备份
			log.info("fileHouse {} 已达到备份数量，不需要再进行备份", fileHouseId);
			return;
		}
		Client srcClient = clients.stream().filter(client -> client.getId().equals(relas.get(0).getClientId())).findFirst().orElse(null);
		if (srcClient == null) {
			log.warn("srcClient[" + relas.get(0).getClientId() + "]不可用");
			return;
		}
		//备份可用服务器，避免备份到同一主机上
		List<Client> backAvaliable = clients.stream().filter(client -> {
			boolean exists = false;
			for (FileHouseRelaClient rela : relas) {
				if (rela.getClientId().equals(client.getId())) {
					exists = true;
					break;
				}
			}
			return !exists;
		}).collect(Collectors.toList());
		log.info("可以备份到资源服务器：{}，文件大小：{}", backAvaliable.stream().map(Client::getUrl).collect(Collectors.toList()), SizeUtils.getReadableSize(fileHouse.getSizeByte()));
		backAvaliable = clientService.filterByFreeSpace(backAvaliable, fileHouse.getSizeByte());
		if (CollectionUtils.isEmpty(backAvaliable)) {
			log.warn("资源" + fileHouseId + "不可备份，资源服务器不够");
			return;
		}
		//数据分片数不能大于当前可用资源服务器数量
		dataFragmentsAmount = Math.min(dataFragmentsAmount, backAvaliable.size() + 1);
		backAvaliable.sort(Comparator.comparing(Client::getUsedPercent));
		int backTime = dataFragmentsAmount - relas.size();
		log.info("数据分片数：{},需要备份次数:{}", dataFragmentsAmount, backTime);
		int successCount = 0;
		for (Client desClient : backAvaliable) {
			if (backTime <= 0) {
				break;
			}
			try {
				copyResource(srcClient, desClient, fileHouse);
				backTime--;
				successCount++;
			} catch (Exception e) {
				int backFails = fileHouse.getBackFails() == null ? 0 : fileHouse.getBackFails();
				backFails++;
				fileHouse.setBackFails(backFails);
				updateById(fileHouse);
				log.error(e.getMessage(), e);
			}
		}
		//更新分片数
		fileHouse.setDataFragmentsCount(relas.size() + successCount);
		updateById(fileHouse);
	}
	
	@Transactional(rollbackFor = {Exception.class})
	public void copyResource(@NotNull Client srcClient, @NotNull Client desClient, @NotNull FileHouse fileHouse) {
		String pathname = fileHouse.getPathname();
		String srcUrl = resourceService.getDesUrl(srcClient, fileHouse);
		log.info("开始备份{}，从{}备份到{},url:{}", pathname, srcClient.getUrl(), desClient.getUrl(), srcUrl);
		IClientApi clientApi = clientApiFactory.getClientApi(desClient);
		backRestTemplate.execute(srcUrl, HttpMethod.GET, null, clientHttpResponse -> {
			InputStream bodyInputStream = clientHttpResponse.getBody();
			LimitInputStream inputStream = new LimitInputStream(bodyInputStream, mosServerProperties.getBackNetWorkLimitSpeed() * 1024);
			long start = System.currentTimeMillis();
			int chunks = IOUtils.convertStreamToByteBufferStream(inputStream, (byteBufferInputStream, index) -> {
				String itemName = fileHouseItemService.getItemName(fileHouse, index);
				try {
					clientApi.upload(byteBufferInputStream, itemName);
				} catch (IOException e) {
					log.error(e.getMessage(), e);
					throw new RuntimeException(e);
				}
			});
			clientApi.mergeFiles(fileHouse.getChunkTempPath(), chunks, pathname, false, fileHouse.getEncode() != null && fileHouse.getEncode());
			long end = System.currentTimeMillis();
			long cost = end - start;
			log.info("备份文件传输{}完成，耗时{}ms，速度:{}", pathname, cost, SpeedUtils.getSpeed(inputStream.getTotalRead(), cost));
			return null;
		});
		FileHouseRelaClient fileHouseRelaClient = new FileHouseRelaClient();
		fileHouseRelaClient.setFileHouseId(fileHouse.getId());
		fileHouseRelaClient.setClientId(desClient.getId());
		fileHouseRelaClientService.save(fileHouseRelaClient);
		log.info("备份{}完成!", pathname);
	}
}
