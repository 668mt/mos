package mt.spring.mos.server.service;

import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.dao.ClientWorkLogMapper;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.entity.po.ClientWorkLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

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
	@Qualifier("httpRestTemplate")
	private RestTemplate httpRestTemplate;
	
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
	
	public synchronized void doLogWork(ClientWorkLog task) {
		task = findById(task.getId());
		if (task.getExeStatus() != ClientWorkLog.ExeStatus.NOT_START) {
			return;
		}
		Client client = clientService.findById(task.getClientId());
		try {
			switch (task.getAction()) {
				case ADD_FILE:
					task.setExeStatus(ClientWorkLog.ExeStatus.IGNORE);
					break;
				case DELETE_FILE:
					List<String> pathnames = (List<String>) task.getParams().get("pathnames");
					for (String pathname : pathnames) {
						client.apis(httpRestTemplate).deleteFile(pathname);
					}
					task.setExeStatus(ClientWorkLog.ExeStatus.SUCCESS);
					break;
				case DELETE_DIR:
					List<String> paths = (List<String>) task.getParams().get("paths");
					for (String path : paths) {
						client.apis(httpRestTemplate).deleteDir(path);
					}
					task.setExeStatus(ClientWorkLog.ExeStatus.SUCCESS);
					break;
				case MOVE_FILE:
					String srcPathname = (String) task.getParams().get("srcPathname");
					String desPathname = (String) task.getParams().get("desPathname");
					client.apis(httpRestTemplate).moveFile(srcPathname, desPathname);
					task.setExeStatus(ClientWorkLog.ExeStatus.SUCCESS);
					break;
			}
		} catch (Exception e) {
			task.setExeStatus(ClientWorkLog.ExeStatus.FAIL);
			task.setMessage(e.getMessage());
			log.error(e.getMessage(), e);
		}
		updateById(task);
	}
}
