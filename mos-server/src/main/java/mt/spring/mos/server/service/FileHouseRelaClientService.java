package mt.spring.mos.server.service;

import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.spring.mos.server.dao.FileHouseRelaClientMapper;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.FileHouseRelaClient;
import mt.utils.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/11/22
 */
@Service
public class FileHouseRelaClientService extends BaseServiceImpl<FileHouseRelaClient> {
	@Autowired
	private FileHouseRelaClientMapper fileHouseRelaClientMapper;
	@Autowired
	@Lazy
	private ClientService clientService;
	
	@Override
	public BaseMapper<FileHouseRelaClient> getBaseMapper() {
		return fileHouseRelaClientMapper;
	}
	
	public List<FileHouseRelaClient> findListByFileHouseId(Long fileHouseId) {
		return findList("fileHouseId", fileHouseId);
	}
	
	public Client findUniqueClient(Long fileHouseId) {
		FileHouseRelaClient fileHouseRelaClient = findUniqueFileHouseRelaClient(fileHouseId);
		String clientId = fileHouseRelaClient.getClientId();
		return clientService.findById(clientId);
	}
	
	public FileHouseRelaClient findUniqueFileHouseRelaClient(Long fileHouseId) {
		List<FileHouseRelaClient> listByFileHouseId = findListByFileHouseId(fileHouseId);
		Assert.state(listByFileHouseId.size() <= 1, "找到多个fileHouseRelaClient");
		Assert.state(listByFileHouseId.size() != 0, "未找到fileHouseRelaClient");
		return listByFileHouseId.get(0);
	}
}
