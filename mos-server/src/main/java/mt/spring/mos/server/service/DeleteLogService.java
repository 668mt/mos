package mt.spring.mos.server.service;

import mt.common.service.BaseServiceImpl;
import mt.spring.mos.server.dao.DeleteLogMapper;
import mt.spring.mos.server.entity.po.FileHouseDeleteLog;
import mt.spring.mos.server.entity.po.Resource;
import mt.utils.common.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Martin
 * @Date 2023/10/6
 */
@Service
public class DeleteLogService extends BaseServiceImpl<FileHouseDeleteLog> {
	@Autowired
	private DeleteLogMapper deleteLogMapper;
	@Autowired
	private FileHouseService fileHouseService;
	
	@Transactional(rollbackFor = Exception.class)
	public void deleteResources(@NotNull List<Resource> resources) {
		List<FileHouseDeleteLog> fileHouseDeleteLogs = new ArrayList<>();
		for (Resource resource : resources) {
			if (resource.getFileHouseId() != null) {
				FileHouseDeleteLog fileHouseDeleteLog = new FileHouseDeleteLog();
				fileHouseDeleteLog.setResourceId(resource.getId());
				fileHouseDeleteLog.setFileHouseId(resource.getFileHouseId());
				fileHouseDeleteLogs.add(fileHouseDeleteLog);
			}
			
			if (resource.getThumbFileHouseId() != null) {
				FileHouseDeleteLog fileHouseDeleteLog = new FileHouseDeleteLog();
				fileHouseDeleteLog.setResourceId(resource.getId());
				fileHouseDeleteLog.setFileHouseId(resource.getThumbFileHouseId());
				fileHouseDeleteLogs.add(fileHouseDeleteLog);
			}
		}
		CollectionUtils.splitBySize(fileHouseDeleteLogs, 200, list -> deleteLogMapper.insertList(fileHouseDeleteLogs));
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void clear(@NotNull FileHouseDeleteLog fileHouseDeleteLog) {
		fileHouseService.clearFileHouse(fileHouseDeleteLog.getFileHouseId());
		deleteById(fileHouseDeleteLog.getId());
	}
	
	@NotNull
	public List<FileHouseDeleteLog> findListBeforeDaysLimit(int beforeDays, int limit) {
		List<FileHouseDeleteLog> fileHouseDeleteLogs = deleteLogMapper.findListBeforeDaysLimit(beforeDays + " 0:0:0", limit);
		if(fileHouseDeleteLogs == null){
			return new ArrayList<>();
		}
		return fileHouseDeleteLogs;
	}
}
