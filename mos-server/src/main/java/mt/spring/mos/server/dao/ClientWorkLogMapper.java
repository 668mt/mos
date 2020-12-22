package mt.spring.mos.server.dao;

import mt.common.mybatis.mapper.BaseMapper;
import mt.spring.mos.server.entity.po.ClientWorkLog;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/10/23
 */
@Repository
public interface ClientWorkLogMapper extends BaseMapper<ClientWorkLog> {
	@Select("select l.* from mos_client_work_log l,mos_client c \n" +
			"where l.client_id = c.id\n" +
			"and exe_status = 'NOT_START' \n" +
			"and c.status = 'UP' order by l.created_date asc")
	List<ClientWorkLog> findNotStartTasks();
}
