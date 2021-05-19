package mt.spring.mos.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author Martin
 * @Date 2021/5/19
 */
@Service
public class ClientLockService {
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Transactional(propagation = Propagation.MANDATORY)
	public void lock(long clientId) {
		jdbcTemplate.queryForList("select 0 from mos_client where id = ? for update", clientId);
	}
}
