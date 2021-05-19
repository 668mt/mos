package mt.spring.mos.server.service;

import mt.common.service.DataLockService;
import org.springframework.beans.factory.InitializingBean;
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
public class FileHouseLockService implements InitializingBean {
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private DataLockService dataLockService;
	
	@Transactional(propagation = Propagation.MANDATORY)
	public void lock(long fileHouseId) {
		jdbcTemplate.queryForList("select 0 from mos_file_house where id = ? for update", fileHouseId);
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void lockGlobal() {
		dataLockService.lock("fileHouseLock", jdbcTemplate);
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		dataLockService.initLock("fileHouseLock", jdbcTemplate);
	}
}
