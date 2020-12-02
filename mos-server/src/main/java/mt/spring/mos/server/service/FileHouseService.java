package mt.spring.mos.server.service;

import lombok.extern.slf4j.Slf4j;
import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.sdk.utils.Assert;
import mt.spring.mos.server.dao.FileHouseMapper;
import mt.spring.mos.server.dao.RelaClientResourceMapper;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.dto.MergeFileResult;
import mt.spring.mos.server.entity.po.*;
import mt.spring.mos.server.entity.vo.BackVo;
import mt.spring.mos.server.listener.ClientWorkLogEvent;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
	private BucketService bucketService;
	@Autowired
	@Lazy
	private FileHouseItemService fileHouseItemService;
	@Autowired
	@Qualifier("httpRestTemplate")
	private RestTemplate httpRestTemplate;
	@Autowired
	@Lazy
	private ResourceService resourceService;
	@Autowired
	private RelaClientResourceMapper relaClientResourceMapper;
	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;
	@Autowired
	private RedissonClient redissonClient;
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
	private CloseableHttpClient httpClient;
	
	@Override
	public BaseMapper<FileHouse> getBaseMapper() {
		return fileHouseMapper;
	}
	
	public FileHouse findByMd5AndSize(String md5, long size) {
		LockCallback<FileHouse> lockCallback = () -> {
			List<Filter> filters = new ArrayList<>();
			filters.add(new Filter("md5", Filter.Operator.eq, md5));
			filters.add(new Filter("sizeByte", Filter.Operator.eq, size));
			return findOneByFilters(filters);
		};
		return doWithLock(md5, LockCallback.LockType.READ, 10, lockCallback);
	}
	
	@Transactional
	public FileHouse getOrCreateFileHouse(String md5, long size, Integer chunks) {
		return doWithLock(md5, LockCallback.LockType.WRITE, 10, () -> {
			FileHouse fileHouse = findByMd5AndSize(md5, size);
			if (fileHouse == null) {
				Client client = clientService.findRandomAvalibleClientForUpload(size);
				Assert.notNull(client, "无可用的存储服务器");
				fileHouse = new FileHouse();
				fileHouse.setMd5(md5);
				fileHouse.setSizeByte(size);
				fileHouse.setChunks(chunks);
				fileHouse.setFileStatus(FileHouse.FileStatus.UPLOADING);
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMM");
				String path = dateFormat.format(new Date());
				fileHouse.setPathname("/" + path + "/" + md5);
				save(fileHouse);
				FileHouseRelaClient fileHouseRelaClient = new FileHouseRelaClient();
				fileHouseRelaClient.setFileHouseId(fileHouse.getId());
				fileHouseRelaClient.setClientId(client.getClientId());
				fileHouseRelaClientService.save(fileHouseRelaClient);
			}
			return fileHouse;
		});
	}
	
	@Transactional
	public void clearFileHouse(FileHouse fileHouse) {
		clearFileHouse(fileHouse, true);
	}
	
	@Transactional
	public void clearFileHouse(FileHouse fileHouse, boolean checkLastModified) {
		log.info("清除资源：{}", fileHouse.getPathname());
		doWithLock(fileHouse.getMd5(), LockCallback.LockType.WRITE, 10, () -> {
			FileHouse lockedFileHouse = findById(fileHouse.getId());
			int countInUsed = resourceService.count(Collections.singletonList(new Filter("fileHouseId", eq, fileHouse.getId())));
			Assert.state(countInUsed == 0, "资源" + lockedFileHouse.getPathname() + "还在被使用，不能清除");
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
			return null;
		});
	}
	
	public <T> T doWithLock(String md5, LockCallback.LockType lockType, int lockMinutes, LockCallback<T> lockCallback) {
		String key = "fileHouse-" + md5;
		RLock lock = null;
		try {
			RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(key);
			lock = lockType == LockCallback.LockType.READ ? readWriteLock.readLock() : readWriteLock.writeLock();
			lock.lock(lockMinutes, TimeUnit.MINUTES);
			return lockCallback.afterLocked();
		} finally {
			if (lock != null) {
				lock.unlock();
			}
		}
	}
	
	interface LockCallback<T> {
		enum LockType {
			READ, WRITE
		}
		
		T afterLocked();
	}
	
	public interface MergeDoneCallback {
		void callback(FileHouse fileHouse);
	}
	
	@Transactional
	@Async
	public Future<FileHouse> mergeFiles(FileHouse fileHouse, boolean updateMd5, MergeDoneCallback mergeDoneCallback) {
		Assert.notNull(fileHouse, "fileHouse不能为空");
		log.info("开始合并文件：{}", fileHouse.getPathname());
		return doWithLock(fileHouse.getMd5(), LockCallback.LockType.WRITE, 10, () -> {
			try {
				Assert.state(fileHouse.getFileStatus() == FileHouse.FileStatus.UPLOADING, "文件" + fileHouse.getPathname() + "已合并完成，无须再次合并");
				int chunks = fileHouseItemService.countItems(fileHouse.getId());
				Assert.state(fileHouse.getChunks() == chunks, "文件" + fileHouse.getPathname() + "还未上传完整，分片数：" + fileHouse.getChunks() + "，已上传分片数：" + chunks);
				List<FileHouseRelaClient> fileHouseRelaClients = fileHouseRelaClientService.findListByFileHouseId(fileHouse.getId());
				Assert.state(fileHouseRelaClients.size() == 1, "资源服务器异常，当前资源：" + fileHouseRelaClients.size());
				FileHouseRelaClient fileHouseRelaClient = fileHouseRelaClients.get(0);
				//合并
				Client client = clientService.findById(fileHouseRelaClient.getClientId());
				Assert.state(clientService.isAlive(client), "存储服务器不可用");
				MergeFileResult mergeFileResult = client.apis(httpRestTemplate).mergeFiles(fileHouse.getChunkTempPath(), fileHouse.getChunks(), fileHouse.getPathname(), updateMd5, true);
				fileHouse.setEncode(true);
				fileHouse.setSizeByte(mergeFileResult.getLength());
				fileHouseItemService.deleteByFilters(Collections.singletonList(new Filter("fileHouseId", eq, fileHouse.getId())));
				if (updateMd5) {
					String md5 = mergeFileResult.getMd5();
					log.info("更新的md5：{}，length:{}", md5, mergeFileResult.getLength());
					FileHouse findFileHouse = findByMd5AndSize(md5, mergeFileResult.getLength());
					if (findFileHouse != null && !findFileHouse.getId().equals(fileHouse.getId()) && findFileHouse.getFileStatus() == FileHouse.FileStatus.OK) {
						log.info("已存在相同的文件，删除此文件");
						clearFileHouse(fileHouse, false);
						if (mergeDoneCallback != null) {
							mergeDoneCallback.callback(findFileHouse);
						}
						return new AsyncResult<>(findFileHouse);
					} else {
						fileHouse.setMd5(md5);
					}
				}
				fileHouse.setFileStatus(FileHouse.FileStatus.OK);
				updateById(fileHouse);
				log.info("文件合并完成：{}", fileHouse.getPathname());
				if (mergeDoneCallback != null) {
					mergeDoneCallback.callback(fileHouse);
				}
				return new AsyncResult<>(fileHouse);
			} catch (RuntimeException e) {
				log.error(e.getMessage(), e);
				throw e;
			}
		});
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
		log.info("转换{}", resource.getPathname());
		Dir dir = dirService.findById(resource.getDirId());
		Bucket bucket = bucketService.findById(dir.getBucketId());
		List<RelaClientResource> list = relaClientResourceMapper.findList("resourceId", resource.getId());
		String srcPathname = resourceService.getDesPathname(bucket, resource);
		List<Client> clients = list.stream().map(relaClientResource -> clientService.findById(relaClientResource.getClientId())).collect(Collectors.toList());
		Optional<Client> any = clients.stream().filter(client -> clientService.isAlive(client)).findAny();
		Assert.state(any.isPresent(), "无可用存储服务器");
		Client aliveClient = any.get();
		String md5 = aliveClient.apis(httpRestTemplate).md5(srcPathname);
		long size = resource.getSizeByte();
		String path = new SimpleDateFormat("yyyyMM").format(resource.getCreatedDate());
		String desPathname = "/" + path + "/" + md5;
		FileHouse fileHouse = findByMd5AndSize(md5, size);
		if (fileHouse == null || fileHouse.getFileStatus() == FileHouse.FileStatus.UPLOADING) {
			if (fileHouse != null && fileHouse.getFileStatus() == FileHouse.FileStatus.UPLOADING) {
				log.info("转换中发现有未完成的上传，清除未完成的上传");
				clearFileHouse(fileHouse, false);
			}
			log.info("移动文件");
			aliveClient.apis(httpRestTemplate).moveFile(srcPathname, desPathname, true);
			fileHouse = new FileHouse();
			fileHouse.setChunks(1);
			fileHouse.setMd5(md5);
			fileHouse.setSizeByte(size);
			fileHouse.setFileStatus(FileHouse.FileStatus.OK);
			fileHouse.setPathname(desPathname);
			log.info("保存新fileHouse:{}", desPathname);
			save(fileHouse);
			FileHouseRelaClient fileHouseRelaClient = new FileHouseRelaClient();
			fileHouseRelaClient.setClientId(aliveClient.getClientId());
			fileHouseRelaClient.setFileHouseId(fileHouse.getId());
			fileHouseRelaClientService.save(fileHouseRelaClient);
		} else {
			log.info("flleHouse已存在，删除原文件");
			aliveClient.apis(httpRestTemplate).deleteFile(srcPathname);
		}
		resource.setFileHouseId(fileHouse.getId());
		resourceService.updateByIdSelective(resource);
		
		for (RelaClientResource relaClientResource : list) {
			relaClientResourceMapper.deleteByPrimaryKey(relaClientResource);
		}
		clients.stream().filter(client -> !client.getClientId().equalsIgnoreCase(aliveClient.getClientId()))
				.forEach(client -> applicationEventPublisher.publishEvent(new ClientWorkLogEvent(this, ClientWorkLog.Action.DELETE_FILE, ClientWorkLog.ExeStatus.NOT_START, client.getClientId(), srcPathname)));
	}
	
	/**
	 * 查询需要备份的数据，当前数据小于数据分片数
	 *
	 * @return
	 */
	public List<BackVo> findNeedBackFileHouses() {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("status", Filter.Operator.eq, Client.ClientStatus.UP));
		//查询存活的服务
		int count = clientService.count(filters);
		//备份数不能大于存活数
		return fileHouseMapper.findNeedBackResourceIds(count);
	}
	
	
	/**
	 * 备份资源
	 *
	 * @param backVo
	 */
	@Transactional(rollbackFor = {Exception.class})
	public void backFileHouse(BackVo backVo) {
		Long fileHouseId = backVo.getFileHouseId();
		FileHouse fileHouse = findById(fileHouseId);
		doWithLock(fileHouse.getMd5(), LockCallback.LockType.READ, 30, () -> {
			Integer dataFragmentsAmount = backVo.getDataFragmentsAmount();
			List<Client> clients = clientService.findAvaliableClients();
			Assert.notEmpty(clients, "无可用资源服务器");
			List<FileHouseRelaClient> relas = fileHouseRelaClientService.findListByFileHouseId(fileHouseId);
			Assert.notEmpty(relas, "资源不存在");
			if (relas.size() >= dataFragmentsAmount) {
				//已经达到数据分片数量了，不需要再进行备份
				log.info("fileHouse {} 已达到备份数量，不需要再进行备份", fileHouseId);
				return null;
			}
			//数据分片数不能大于当前可用资源服务器数量
			dataFragmentsAmount = clients.size() > dataFragmentsAmount ? dataFragmentsAmount : clients.size();
			Client srcClient = clients.stream().filter(client -> client.getClientId().equalsIgnoreCase(relas.get(0).getClientId())).findFirst().orElse(null);
			Assert.notNull(srcClient, "srcClient[" + relas.get(0).getClientId() + "]不可用");
			//备份可用服务器，避免备份到同一主机上
			List<Client> backAvaliable = clients.stream().filter(client -> {
				boolean exists = false;
				for (FileHouseRelaClient rela : relas) {
					if (rela.getClientId().equalsIgnoreCase(client.getClientId())) {
						exists = true;
						break;
					}
				}
				return !exists;
			}).collect(Collectors.toList());
			Assert.notEmpty(backAvaliable, "资源" + fileHouseId + "不可备份，资源服务器不够");
			backAvaliable.sort(Comparator.comparing(Client::getUsedPercent));
			int backTime = dataFragmentsAmount - relas.size();
			log.info("数据分片数：{},需要备份次数:{}", dataFragmentsAmount, backTime);
			for (Client desClient : backAvaliable) {
				if (backTime <= 0) {
					break;
				}
				try {
					copyResource(srcClient, desClient, fileHouse);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
				backTime--;
			}
			return null;
		});
	}
	
	public void copyResource(Client srcClient, Client desClient, FileHouse fileHouse) {
		String pathname = fileHouse.getPathname();
		String srcUrl = srcClient.getUrl() + "/mos" + pathname;
		log.info("开始备份{}，从{}备份到{}", pathname, srcClient.getUrl(), desClient.getUrl());
		backRestTemplate.execute(srcUrl, HttpMethod.GET, null, clientHttpResponse -> {
			InputStream inputStream = clientHttpResponse.getBody();
			desClient.apis(httpRestTemplate).upload(httpClient, inputStream, pathname);
			FileHouseRelaClient fileHouseRelaClient = new FileHouseRelaClient();
			fileHouseRelaClient.setFileHouseId(fileHouse.getId());
			fileHouseRelaClient.setClientId(desClient.getClientId());
			fileHouseRelaClientService.save(fileHouseRelaClient);
			log.info("备份{}完成!", pathname);
			return null;
		});
	}
}
