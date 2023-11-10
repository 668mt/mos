package mt.spring.mos.server.service;

import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.dao.ClientWorkLogMapper;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.ClientWorkLog;
import mt.spring.mos.server.service.clientapi.ClientApiFactory;
import mt.spring.mos.server.service.clientapi.IClientApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @Author Martin
 * @Date 2020/10/23
 */
@Service
@Slf4j
public class ClientWorkLogService extends BaseServiceImpl<ClientWorkLog> {
	@Autowired
	private ClientWorkLogMapper clientWorkLogMapper;
	@Autowired
	private ClientService clientService;
	@Autowired
	private ClientApiFactory clientApiFactory;
	@Autowired
	private LockService lockService;
	
	@Override
	public BaseMapper<ClientWorkLog> getBaseMapper() {
		return clientWorkLogMapper;
	}
	
	public List<ClientWorkLog> findTasksByClientId(Long clientId) {
		PageHelper.orderBy("created_date asc");
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("exeStatus", Filter.Operator.eq, ClientWorkLog.ExeStatus.NOT_START));
		filters.add(new Filter("clientId", Filter.Operator.eq, clientId));
		return findByFilters(filters);
	}
	
	public List<ClientWorkLog> findTasks() {
		return clientWorkLogMapper.findNotStartTasks();
	}
	
	public void addWorkLog(long clientId, @NotNull ClientWorkLog.Action action, @Nullable String lockKey, @Nullable Map<String, Object> params) {
		ClientWorkLog clientWorkLog = new ClientWorkLog();
		clientWorkLog.setLockKey(lockKey);
		clientWorkLog.setAction(action);
		clientWorkLog.setClientId(clientId);
		clientWorkLog.setExeStatus(ClientWorkLog.ExeStatus.NOT_START);
		clientWorkLog.setParams(params);
		save(clientWorkLog);
	}
	
	public void addDeleteFile(@NotNull Long clientId, @Nullable String lockKey, @NotNull String pathname) {
		Map<String, Object> params = new HashMap<>();
		params.put("pathnames", Collections.singletonList(pathname));
		addWorkLog(clientId, ClientWorkLog.Action.DELETE_FILE, lockKey, params);
	}
	
	public void addMoveFile(@NotNull Long clientId, @Nullable String lockKey, @NotNull String srcPathname, @NotNull String desPathname) {
		Map<String, Object> params = new HashMap<>();
		params.put("srcPathname", srcPathname);
		params.put("desPathname", desPathname);
		addWorkLog(clientId, ClientWorkLog.Action.MOVE_FILE, lockKey, params);
	}
	
	public void addDeleteDir(@NotNull Long clientId, @Nullable String lockKey, @NotNull String path) {
		Map<String, Object> params = new HashMap<>();
		params.put("paths", Collections.singletonList(path));
		addWorkLog(clientId, ClientWorkLog.Action.DELETE_DIR, lockKey, params);
	}
	
	
	@Transactional(rollbackFor = Exception.class)
	public void deleteByLockKey(@NotNull String lockKey) {
		deleteByFilter(new Filter("lockKey", Filter.Operator.eq, lockKey));
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void doLogWork(Long taskId) {
		ClientWorkLog task = findOneByFilter(new Filter("id", Filter.Operator.eq, taskId), true);
		if (task == null || task.getExeStatus() != ClientWorkLog.ExeStatus.NOT_START) {
			return;
		}
		String lockLey = task.getLockKey();
		lockService.doWithLock(lockLey, LockService.LockType.WRITE, () -> {
			Client client = clientService.findById(task.getClientId());
			IClientApi clientApi = clientApiFactory.getClientApi(client);
			try {
				switch (task.getAction()) {
					case ADD_FILE:
						task.setExeStatus(ClientWorkLog.ExeStatus.IGNORE);
						break;
					case DELETE_FILE:
						List<String> pathnames = (List<String>) task.getParams().get("pathnames");
						for (String pathname : pathnames) {
							clientApi.deleteFile(pathname);
						}
						task.setExeStatus(ClientWorkLog.ExeStatus.SUCCESS);
						break;
					case DELETE_DIR:
						List<String> paths = (List<String>) task.getParams().get("paths");
						for (String path : paths) {
							clientApi.deleteDir(path);
						}
						task.setExeStatus(ClientWorkLog.ExeStatus.SUCCESS);
						break;
					case MOVE_FILE:
						String srcPathname = (String) task.getParams().get("srcPathname");
						String desPathname = (String) task.getParams().get("desPathname");
						clientApi.moveFile(srcPathname, desPathname);
						task.setExeStatus(ClientWorkLog.ExeStatus.SUCCESS);
						break;
				}
			} catch (Exception e) {
				task.setExeStatus(ClientWorkLog.ExeStatus.FAIL);
				task.setMessage(e.getMessage());
				log.error(e.getMessage(), e);
			}
			updateById(task);
		});
	}
	
}
