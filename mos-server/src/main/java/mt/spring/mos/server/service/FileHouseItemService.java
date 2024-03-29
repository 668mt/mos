package mt.spring.mos.server.service;

import lombok.extern.slf4j.Slf4j;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.FileHouse;
import mt.spring.mos.server.entity.po.FileHouseItem;
import mt.spring.mos.server.entity.po.FileHouseRelaClient;
import mt.spring.mos.server.service.clientapi.ClientApiFactory;
import mt.spring.mos.server.service.clientapi.IClientApi;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author Martin
 * @Date 2020/11/21
 */
@Service
@Slf4j
public class FileHouseItemService extends BaseServiceImpl<FileHouseItem> {
	@Autowired
	private ClientService clientService;
	@Autowired
	private FileHouseService fileHouseService;
	@Autowired
	@Lazy
	private FileHouseRelaClientService fileHouseRelaClientService;
	@Autowired
	private ClientApiFactory clientApiFactory;
	@Autowired
	private AuditService auditService;
	@Autowired
	private FileHouseLockService fileHouseLockService;
	
	
	public FileHouseItem findByMd5AndSize(long fileHouseId, String md5) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("md5", Filter.Operator.eq, md5));
		filters.add(new Filter("fileHouseId", Filter.Operator.eq, fileHouseId));
		return findOneByFilters(filters);
	}
	
	public String getItemName(FileHouse fileHouse, int index) {
		return fileHouse.getChunkTempPath() + "/part" + index;
	}
	
	@Autowired
	private RedissonClient redissonClient;
	
	@Transactional
	public void upload(long bucketId, long fileHouseId, String chunkMd5, int chunkIndex, InputStream inputStream) throws IOException {
		Assert.notNull(chunkMd5, "chunkMd5不能为空");
		Assert.notNull(inputStream, "上传文件不能为空");
		long start = System.currentTimeMillis();
		RLock lock = redissonClient.getLock("fileHouse-" + fileHouseId);
		try {
			lock.lock();
			log.info("上传文件分片：{},{}", chunkMd5, chunkIndex);
			FileHouse fileHouse = fileHouseService.findById(fileHouseId);
			Assert.notNull(fileHouse, "fileHouse不存在:" + fileHouseId);
			if (fileHouse.getFileStatus() == FileHouse.FileStatus.OK) {
				log.info("fileHouse已完成：{}，跳过分片上传", fileHouseId);
				return;
			}
			FileHouseItem fileHouseItem = findByMd5AndSize(fileHouseId, chunkMd5);
			if (fileHouseItem != null) {
				deleteById(fileHouseItem.getId());
			}
			List<FileHouseRelaClient> fileHouseRelaClients = fileHouseRelaClientService.findListByFileHouseId(fileHouseId);
			Assert.state(fileHouseRelaClients.size() == 1, "资源服务器异常，当前资源：" + fileHouseRelaClients.size());
			Client client = clientService.findById(fileHouseRelaClients.get(0).getClientId());
			Assert.state(clientService.isAlive(client), "存储服务器不可用");
			List<Filter> filters = new ArrayList<>();
			filters.add(new Filter("chunkIndex", Filter.Operator.eq, chunkIndex));
			filters.add(new Filter("fileHouseId", Filter.Operator.eq, fileHouseId));
			FileHouseItem findFileHouseItem = findOneByFilters(filters);
			if (findFileHouseItem != null) {
				deleteById(findFileHouseItem);
			}
			int chunkSize = inputStream.available();
			IClientApi clientApi = clientApiFactory.getClientApi(client);
			clientApi.upload(inputStream, getItemName(fileHouse, chunkIndex));
			auditService.writeBytesRecord(bucketId, chunkSize);
			fileHouseItem = new FileHouseItem();
			fileHouseItem.setChunkIndex(chunkIndex);
			fileHouseItem.setFileHouseId(fileHouse.getId());
			fileHouseItem.setSizeByte((long) chunkSize);
			fileHouseItem.setMd5(chunkMd5);
			save(fileHouseItem);
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
			log.info("fileHouseId upload用时：{}ms", System.currentTimeMillis() - start);
		}
	}
	
	public int countItems(long fileHouseId) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("fileHouseId", Filter.Operator.eq, fileHouseId));
		return count(filters);
	}
	
	@Transactional
	public void deleteByFileHouseId(Long fileHouseId) {
		deleteByFilters(Collections.singletonList(new Filter("fileHouseId", Filter.Operator.eq, fileHouseId)));
	}
	
}
