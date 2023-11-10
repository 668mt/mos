package mt.spring.mos.server.service;

import mt.common.service.BaseServiceImpl;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.FileHouseRelaClient;
import mt.utils.common.Assert;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/11/22
 */
@Service
public class FileHouseRelaClientService extends BaseServiceImpl<FileHouseRelaClient> {
	@Autowired
	@Lazy
	private ClientService clientService;
	
	public List<FileHouseRelaClient> findListByFileHouseId(Long fileHouseId) {
		return findList("fileHouseId", fileHouseId);
	}
	
	public Client findUniqueClient(Long fileHouseId) {
		FileHouseRelaClient fileHouseRelaClient = findUniqueFileHouseRelaClient(fileHouseId);
		Long clientId = fileHouseRelaClient.getClientId();
		return clientService.findById(clientId);
	}
	
	public FileHouseRelaClient findUniqueFileHouseRelaClient(Long fileHouseId) {
		List<FileHouseRelaClient> listByFileHouseId = findListByFileHouseId(fileHouseId);
		Assert.state(listByFileHouseId.size() <= 1, "找到多个fileHouseRelaClient");
		Assert.state(listByFileHouseId.size() != 0, "未找到fileHouseRelaClient");
		return listByFileHouseId.get(0);
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void bind(@NotNull Long fileHouseId, @NotNull Long clientId) {
		FileHouseRelaClient fileHouseRelaClient = new FileHouseRelaClient();
		fileHouseRelaClient.setClientId(clientId);
		fileHouseRelaClient.setFileHouseId(fileHouseId);
		save(fileHouseRelaClient);
	}
}
