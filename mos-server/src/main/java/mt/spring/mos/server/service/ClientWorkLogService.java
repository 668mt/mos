package mt.spring.mos.server.service;

import com.github.pagehelper.PageHelper;
import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.dao.ClientWorkLogMapper;
import mt.spring.mos.server.entity.po.ClientWorkLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Martin
 * @Date 2020/10/23
 */
@Service
public class ClientWorkLogService extends BaseServiceImpl<ClientWorkLog> {
	@Autowired
	private ClientWorkLogMapper clientWorkLogMapper;
	@Autowired
	private ClientService clientService;
	
	@Override
	public BaseMapper<ClientWorkLog> getBaseMapper() {
		return clientWorkLogMapper;
	}
	
	public List<ClientWorkLog> findTasksByClientId(String clientId) {
		PageHelper.orderBy("created_date asc");
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("exeStatus", Filter.Operator.eq, ClientWorkLog.ExeStatus.NOT_START));
		filters.add(new Filter("clientId", Filter.Operator.eq, clientId));
		return findByFilters(filters);
	}
	
	public List<ClientWorkLog> findTasks() {
		return clientWorkLogMapper.findNotStartTasks();
	}
}
