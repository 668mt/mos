package mt.spring.mos.server.service;

import lombok.extern.slf4j.Slf4j;
import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.base.utils.IOUtils;
import mt.spring.mos.server.dao.FileHouseMapper;
import mt.spring.mos.server.dao.RelaClientResourceMapper;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.dto.InitUploadDto;
import mt.spring.mos.server.entity.dto.MergeFileResult;
import mt.spring.mos.server.entity.po.*;
import mt.spring.mos.server.entity.vo.BackVo;
import mt.spring.mos.server.listener.ClientWorkLogEvent;
import mt.spring.mos.server.service.clientapi.ClientApiFactory;
import mt.spring.mos.server.service.clientapi.IClientApi;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static mt.common.tkmapper.Filter.Operator.eq;
import static mt.common.tkmapper.Filter.Operator.in;

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
	private BucketService bucketService;
	@Autowired
	@Lazy
	private FileHouseItemService fileHouseItemService;
	@Autowired
	@Lazy
	private ResourceService resourceService;
	@Autowired
	private RelaClientResourceMapper relaClientResourceMapper;
	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;
	@Autowired
	@Lazy
	private FileHouseRelaClientService fileHouseRelaClientService;
	@Autowired
	private MosServerProperties mosServerProperties;
	@Autowired
	@Qualifier("backRestTemplate")
	private RestTemplate backRestTemplate;
	@Autowired
	@Lazy
	private DirService dirService;
	@Autowired
	private ClientApiFactory clientApiFactory;
	@Autowired
	private FileHouseLockService fileHouseLockService;
	@Autowired
	private TransactionTemplate transactionTemplate;
	
	@Override
	public BaseMapper<FileHouse> getBaseMapper() {
		return fileHouseMapper;
	}
	
	public FileHouse findByMd5AndSize(String md5, long size) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("md5", Filter.Operator.eq, md5));
		filters.add(new Filter("sizeByte", Filter.Operator.eq, size));
		return findOneByFilters(filters);
	}
	
	private String getUploadPathname(String md5) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMM");
		String path = dateFormat.format(new Date());
		return "/" + path + "/" + md5;
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void createFileHouseIfNotExists(String md5, long size, Integer chunks) {
		fileHouseLockService.lockGlobal();
		FileHouse fileHouse = findByMd5AndSize(md5, size);
		if (fileHouse != null) {
			return;
		}
		Client client = clientService.findRandomAvalibleClientForUpload(size);
		Assert.notNull(client, "无可用的存储服务器");
		fileHouse = new FileHouse();
		fileHouse.setMd5(md5);
		fileHouse.setSizeByte(size);
		fileHouse.setChunks(chunks);
		fileHouse.setFileStatus(FileHouse.FileStatus.UPLOADING);
		fileHouse.setPathname(getUploadPathname(md5));
		fileHouse.setDataFragmentsCount(0);
		save(fileHouse);
		FileHouseRelaClient fileHouseRelaClient = new FileHouseRelaClient();
		fileHouseRelaClient.setFileHouseId(fileHouse.getId());
		fileHouseRelaClient.setClientId(client.getId());
		fileHouseRelaClientService.save(fileHouseRelaClient);
	}
	
	@Transactional
	public void clearFileHouse(FileHouse fileHouse, boolean checkLastModified) {
		log.info("清除资源：{}", fileHouse.getPathname());
		fileHouseLockService.lockForUpdate(fileHouse.getId(), () -> {
			FileHouse lockedFileHouse = findById(fileHouse.getId());
			int countInUsed = resourceService.count(Collections.singletonList(new Filter("fileHouseId", eq, fileHouse.getId())));
			int thumbCountInUsed = resourceService.count(Collections.singletonList(new Filter("thumbFileHouseId", eq, fileHouse.getId())));
			Assert.state(countInUsed == 0 && thumbCountInUsed == 0, "资源" + lockedFileHouse.getPathname() + "还在被使用，不能清除");
			if (checkLastModified) {
				long lastModified = 0;
				if (lockedFileHouse.getUpdatedDate() != null) {
					lastModified = lockedFileHouse.getUpdatedDate().getTime();
				} else if (lockedFileHouse.getCreatedDate() != null) {
					lastModified = lockedFileHouse.getCreatedDate().getTime();
				}
				Assert.state(System.currentTimeMillis() - lastModified > mosServerProperties.getDeleteRecentDaysNotUsed() * 3600 * 24 * 1000, "不能删除最近" + mosServerProperties.getDeleteRecentDaysNotUsed() + "天使用过的资源");
			}
			List<FileHouseRelaClient> listByFileHouseId = fileHouseRelaClientService.findListByFileHouseId(lockedFileHouse.getId());
			if (CollectionUtils.isNotEmpty(listByFileHouseId)) {
				listByFileHouseId.forEach(fileHouseRelaClient -> {
					applicationEventPublisher.publishEvent(new ClientWorkLogEvent(this, ClientWorkLog.Action.DELETE_FILE, ClientWorkLog.ExeStatus.NOT_START, fileHouseRelaClient.getClientId(), lockedFileHouse.getPathname()));
					applicationEventPublisher.publishEvent(new ClientWorkLogEvent(this, ClientWorkLog.Action.DELETE_DIR, ClientWorkLog.ExeStatus.NOT_START, fileHouseRelaClient.getClientId(), lockedFileHouse.getChunkTempPath()));
					fileHouseRelaClientService.deleteById(fileHouseRelaClient);
				});
			}
			deleteById(lockedFileHouse);
		});
	}
	
	public interface MergeDoneCallback {
		void callback(FileHouse fileHouse);
	}
	
	private AsyncResult<FileHouse> executeCallback(FileHouse fileHouse, MergeDoneCallback mergeDoneCallback) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				if (mergeDoneCallback != null) {
					mergeDoneCallback.callback(fileHouse);
				}
			}
		});
		return new AsyncResult<>(fileHouse);
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void initFileHouse(FileHouse fileHouse, String totalMd5, Long totalSize, Integer chunks, String pathname, InitUploadDto initUploadDto) {
		long start = System.currentTimeMillis();
		try {
			if (fileHouse == null) {
				createFileHouseIfNotExists(totalMd5, totalSize, chunks);
				return;
			}
			long fileHouseId = fileHouse.getId();
			fileHouseLockService.lockForUpdate(fileHouseId, () -> {
				FileHouse findFileHouse = findById(fileHouseId);
				if (findFileHouse.getFileStatus() == FileHouse.FileStatus.OK) {
					return;
				}
				//还未上传完成
				FileHouseRelaClient fileHouseRelaClient = fileHouseRelaClientService.findUniqueFileHouseRelaClient(findFileHouse.getId());
				//原客户端
				Client client = clientService.findById(fileHouseRelaClient.getClientId());
				if (!clientService.isAlive(client)) {
					//原client不可用了，删掉原来的分片，重新找一个可用的client
					Client newClient = clientService.findRandomAvalibleClientForUpload(totalSize);
					log.info("{}:原client[{}]不可用，重新分配新的client[{}]", pathname, client.getName(), newClient.getName());
					//删掉原来上传的分片
					fileHouseItemService.deleteByFileHouseId(findFileHouse.getId());
					//新增删除原分片的任务
					applicationEventPublisher.publishEvent(new ClientWorkLogEvent(this, ClientWorkLog.Action.DELETE_DIR, ClientWorkLog.ExeStatus.NOT_START, client.getId(), findFileHouse.getChunkTempPath()));
					//设置新的client
					fileHouseRelaClient.setClientId(newClient.getId());
					fileHouseRelaClientService.updateById(fileHouseRelaClient);
				} else {
					List<FileHouseItem> items = fileHouseItemService.findList("fileHouseId", findFileHouse.getId());
					if (CollectionUtils.isEmpty(items)) {
						return;
					}
					
					if (!Objects.equals(findFileHouse.getChunks(), chunks) || !Objects.equals(items.size(), chunks)) {
						//分片有变更，删除原有分片
						fileHouseItemService.deleteByFileHouseId(fileHouseId);
						clientApiFactory.getClientApi(client).deleteDir(findFileHouse.getChunkTempPath());
						findFileHouse.setChunks(chunks);
						updateByIdSelective(findFileHouse);
						return;
					}
					
					//查找已经上传过的分片
					Map<String, Integer> map = new HashMap<>();
					for (FileHouseItem item : items) {
						String itemPathname = fileHouseItemService.getItemName(findFileHouse, item.getChunkIndex());
						map.put(itemPathname, item.getChunkIndex());
					}
					List<String> pathnames = new ArrayList<>(map.keySet());
					Map<String, Boolean> result = clientApiFactory.getClientApi(client).isExists(pathnames);
					List<Integer> existsChunkIndex = new ArrayList<>();
					for (Map.Entry<String, Boolean> stringBooleanEntry : result.entrySet()) {
						if (stringBooleanEntry.getValue()) {
							existsChunkIndex.add(map.get(stringBooleanEntry.getKey()));
						}
					}
					initUploadDto.setExistedChunkIndexs(existsChunkIndex);
					List<Long> deleteItemIds = items.stream().filter(fileHouseItem -> !existsChunkIndex.contains(fileHouseItem.getChunkIndex())).map(FileHouseItem::getId).collect(Collectors.toList());
					if (CollectionUtils.isNotEmpty(deleteItemIds)) {
						fileHouseItemService.deleteByFilter(new Filter("id", in, deleteItemIds));
					}
				}
			});
		} finally {
			log.info("initFileHouse用时：{}ms", System.currentTimeMillis() - start);
		}
	}
	
	@Async
	public Future<FileHouse> mergeFiles(Long fileHouseId, Integer updateChunks, boolean updateMd5, MergeDoneCallback mergeDoneCallback) {
		long start = System.currentTimeMillis();
		try {
			FileHouse result = transactionTemplate.execute(transactionStatus -> {
				return fileHouseLockService.lockForUpdate(fileHouseId, () -> {
					FileHouse fileHouse = findById(fileHouseId);
					Assert.notNull(fileHouse, "fileHouse不能为空");
					if (updateChunks != null) {
						fileHouse.setChunks(updateChunks);
					}
					String pathname = fileHouse.getPathname();
					log.info("开始合并文件：{}", pathname);
					try {
						if (fileHouse.getFileStatus() == FileHouse.FileStatus.OK) {
							return fileHouse;
						}
						int chunks = fileHouseItemService.countItems(fileHouse.getId());
						Assert.state(fileHouse.getChunks() == chunks, "合并失败,文件" + pathname + "还未上传完整，分片数：" + fileHouse.getChunks() + "，已上传分片数：" + chunks);
						List<FileHouseRelaClient> fileHouseRelaClients = fileHouseRelaClientService.findListByFileHouseId(fileHouse.getId());
						Assert.state(fileHouseRelaClients.size() == 1, "资源服务器异常，当前资源：" + fileHouseRelaClients.size());
						FileHouseRelaClient fileHouseRelaClient = fileHouseRelaClients.get(0);
						//合并
						Client client = clientService.findById(fileHouseRelaClient.getClientId());
						Assert.state(clientService.isAlive(client), "存储服务器不可用");
						IClientApi clientApi = clientApiFactory.getClientApi(client);
						MergeFileResult mergeFileResult = clientApi.mergeFiles(fileHouse.getChunkTempPath(), fileHouse.getChunks(), pathname, updateMd5, true);
						long length = mergeFileResult.getLength();
						String totalMd5 = mergeFileResult.getMd5();
						fileHouse.setEncode(true);
						fileHouse.setSizeByte(length);
						fileHouseItemService.deleteByFilters(Collections.singletonList(new Filter("fileHouseId", eq, fileHouse.getId())));
						if (updateMd5) {
							if (totalMd5 == null) {
								totalMd5 = clientApi.md5(pathname);
							}
							log.info("更新的md5：{}，length:{}", totalMd5, length);
							FileHouse findFileHouse = findByMd5AndSize(totalMd5, length);
							if (findFileHouse != null && !findFileHouse.getId().equals(fileHouse.getId()) && findFileHouse.getFileStatus() == FileHouse.FileStatus.OK) {
								log.info("已存在相同的文件，删除此文件");
								clearFileHouse(fileHouse, false);
								return fileHouse;
							} else {
								fileHouse.setMd5(totalMd5);
							}
						}
						fileHouse.setFileStatus(FileHouse.FileStatus.OK);
						updateById(fileHouse);
						log.info("文件合并完成：{}", pathname);
						return fileHouse;
					} catch (Exception e) {
						log.error(e.getMessage(), e);
						throw e;
					}
				});
			});
			if (result != null && mergeDoneCallback != null) {
				mergeDoneCallback.callback(result);
			}
			return new AsyncResult<>(result);
		} finally {
			long cost = System.currentTimeMillis() - start;
			log.info("fileHouse{}合并完成：{}ms", fileHouseId, cost);
		}
	}
	
	public List<FileHouse> findNotUsedFileHouseList(int beforeDays) {
		return fileHouseMapper.findNotUsedFileHouseList(beforeDays + " 0:0:0");
	}
	
	/**
	 * 把传统的资源转换为文件仓库
	 *
	 * @param resource 传统的资源
	 */
	@Transactional
	public void convertTraditionalToFileHouse(Resource resource) {
		log.info("转换{}", resource.getName());
		Dir dir = dirService.findById(resource.getDirId());
		Bucket bucket = bucketService.findById(dir.getBucketId());
		List<RelaClientResource> list = relaClientResourceMapper.findList("resourceId", resource.getId());
		String srcPathname = resourceService.getDesPathname(bucket.getId(), resource);
		List<Client> clients = list.stream().map(relaClientResource -> clientService.findById(relaClientResource.getClientId())).collect(Collectors.toList());
		Optional<Client> any = clients.stream().filter(client -> clientService.isAlive(client)).findAny();
		Assert.state(any.isPresent(), "无可用存储服务器");
		Client aliveClient = any.get();
		IClientApi clientApi = clientApiFactory.getClientApi(aliveClient);
		String md5 = clientApi.md5(srcPathname);
		long size = resource.getSizeByte();
		String path = new SimpleDateFormat("yyyyMM").format(resource.getCreatedDate());
		String desPathname = "/" + path + "/" + md5;
		FileHouse fileHouse = findByMd5AndSize(md5, size);
		if (fileHouse == null || fileHouse.getFileStatus() == FileHouse.FileStatus.UPLOADING) {
			if (fileHouse != null && fileHouse.getFileStatus() == FileHouse.FileStatus.UPLOADING) {
				log.info("转换中发现有未完成的上传，清除未完成的上传");
				clearFileHouse(fileHouse, false);
			}
			log.info("移动文件{} -> {}", srcPathname, desPathname);
			clientApi.moveFile(srcPathname, desPathname, true);
			fileHouse = new FileHouse();
			fileHouse.setEncode(false);
			fileHouse.setChunks(1);
			fileHouse.setMd5(md5);
			fileHouse.setSizeByte(size);
			fileHouse.setFileStatus(FileHouse.FileStatus.OK);
			fileHouse.setPathname(desPathname);
			fileHouse.setDataFragmentsCount(0);
			log.info("保存新fileHouse:{}", desPathname);
			save(fileHouse);
			FileHouseRelaClient fileHouseRelaClient = new FileHouseRelaClient();
			fileHouseRelaClient.setClientId(aliveClient.getId());
			fileHouseRelaClient.setFileHouseId(fileHouse.getId());
			fileHouseRelaClientService.save(fileHouseRelaClient);
		} else {
			log.info("flleHouse已存在，删除原文件");
			clientApi.deleteFile(srcPathname);
		}
		resource.setFileHouseId(fileHouse.getId());
		resourceService.updateByIdSelective(resource);
		
		for (RelaClientResource relaClientResource : list) {
			relaClientResourceMapper.deleteByPrimaryKey(relaClientResource);
		}
		clients.stream().filter(client -> !client.getId().equals(aliveClient.getId()))
				.forEach(client -> applicationEventPublisher.publishEvent(new ClientWorkLogEvent(this, ClientWorkLog.Action.DELETE_FILE, ClientWorkLog.ExeStatus.NOT_START, client.getId(), srcPathname)));
	}
	
	/**
	 * 查询需要备份的数据，当前数据小于数据分片数
	 *
	 * @return
	 */
	public List<BackVo> findNeedBackFileHouses(int limit) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("status", Filter.Operator.eq, Client.ClientStatus.UP));
		//查询存活的服务
		int count = clientService.count(filters);
		//备份数不能大于存活数
		List<BackVo> needBackFileHouseIds = fileHouseMapper.findNeedBackFileHouseIds(count, limit);
		List<BackVo> needBackThumbFileHouseIds = fileHouseMapper.findNeedBackThumbFileHouseIds(count, limit);
		List<BackVo> list = new ArrayList<>();
		if (needBackFileHouseIds != null) {
			list.addAll(needBackFileHouseIds);
		}
		if (needBackThumbFileHouseIds != null) {
			list.addAll(needBackThumbFileHouseIds);
		}
		return list;
	}
	
	/**
	 * 备份资源
	 *
	 * @param backVo
	 */
	@Transactional(rollbackFor = {Exception.class})
	public void backFileHouse(BackVo backVo) {
		Long fileHouseId = backVo.getFileHouseId();
		fileHouseLockService.lockForRead(fileHouseId);
		FileHouse fileHouse = findById(fileHouseId);
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
		List<Client> backAvaliable = clients.stream()
				.filter(client -> {
					boolean exists = false;
					for (FileHouseRelaClient rela : relas) {
						if (rela.getClientId().equals(client.getId())) {
							exists = true;
							break;
						}
					}
					return !exists;
				})
				.collect(Collectors.toList());
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
	
	@Transactional
	public void copyResource(Client srcClient, Client desClient, FileHouse fileHouse) {
		String pathname = fileHouse.getPathname();
		String srcUrl = resourceService.getDesUrl(srcClient, fileHouse);
		log.info("开始备份{}，从{}备份到{}", pathname, srcClient.getUrl(), desClient.getUrl());
		IClientApi clientApi = clientApiFactory.getClientApi(desClient);
		if (!clientApi.isExists(pathname)) {
			backRestTemplate.execute(srcUrl, HttpMethod.GET, null, clientHttpResponse -> {
				InputStream inputStream = clientHttpResponse.getBody();
				int chunks = IOUtils.convertStreamToByteBufferStream(inputStream, (byteBufferInputStream, index) -> {
					String itemName = fileHouseItemService.getItemName(fileHouse, index);
					try {
						clientApi.upload(byteBufferInputStream, itemName);
					} catch (IOException e) {
						log.error(e.getMessage(), e);
						throw new RuntimeException(e);
					}
				});
				clientApi.mergeFiles(fileHouse.getChunkTempPath(), chunks, pathname, false, fileHouse.getEncode() == null ? false : fileHouse.getEncode());
				return null;
			});
		}
		FileHouseRelaClient fileHouseRelaClient = new FileHouseRelaClient();
		fileHouseRelaClient.setFileHouseId(fileHouse.getId());
		fileHouseRelaClient.setClientId(desClient.getId());
		fileHouseRelaClientService.save(fileHouseRelaClient);
		log.info("备份{}完成!", pathname);
	}
	
	@Transactional(rollbackFor = Exception.class)
	public FileHouse uploadLocalFile(File file) throws Exception {
		try (InputStream md5InputStream = new FileInputStream(file);
			 InputStream inputStream = new FileInputStream(file)) {
			String md5 = DigestUtils.md5Hex(md5InputStream);
			String pathname = getUploadPathname(md5);
			long size = file.length();
			FileHouse fileHouse = findByMd5AndSize(md5, size);
			boolean md5Exists = fileHouse != null && fileHouse.getFileStatus() == FileHouse.FileStatus.OK;
			if (md5Exists) {
				return fileHouse;
			}
			InitUploadDto initUploadDto = new InitUploadDto();
			initFileHouse(fileHouse, md5, size, 1, pathname, initUploadDto);
			fileHouse = findByMd5AndSize(md5, size);
			fileHouseItemService.upload(fileHouse.getId(), md5, 0, inputStream);
			mergeFiles(fileHouse.getId(), 1, false, null);
			return fileHouse;
		}
	}
}
