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
public class FileHouseLockService  {
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
//	@SneakyThrows
//	@Transactional(propagation = Propagation.MANDATORY)
//	public <T> T lockForUpdate(long fileHouseId, @NotNull LockService.LockCallbackWithResult<T> callbackWithResult) {
//		fileHouseService.findOneByFilter(new Filter("id", Filter.Operator.eq, fileHouseId), true);
//		return callbackWithResult.afterLocked();
//	}
//
//	@SneakyThrows
//	@Transactional(propagation = Propagation.MANDATORY)
//	public void lockForUpdate(long fileHouseId, @NotNull LockService.LockCallback lockCallback) {
//		fileHouseService.findOneByFilter(new Filter("id", Filter.Operator.eq, fileHouseId), true);
//		lockCallback.afterLocked();
//	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void lockForRead(long fileHouseId) {
		jdbcTemplate.queryForList("select 0 from mos_file_house where id = ? lock in share mode", fileHouseId);
	}
	
}
