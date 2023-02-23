package mt.spring.mos.server.service;

import lombok.SneakyThrows;
import mt.common.service.DataLockService;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.entity.MosServerProperties;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
	@Autowired
	private LockService lockService;
	@Autowired
	private MosServerProperties mosServerProperties;
	@Autowired
	@Lazy
	private FileHouseService fileHouseService;
	
	@SneakyThrows
	@Transactional(propagation = Propagation.MANDATORY)
	public <T> T lockForUpdate(long fileHouseId, @NotNull LockService.LockCallbackWithResult<T> callbackWithResult) {
		fileHouseService.findOneByFilter(new Filter("id", Filter.Operator.eq, fileHouseId), true);
		return callbackWithResult.afterLocked();
//		String key = mosServerProperties.getRedisPrefix() + "fileHouse-" + fileHouseId;
//		return lockService.doWithLock(key, LockService.LockType.WRITE, callbackWithResult);
	}
	
	@SneakyThrows
	@Transactional(propagation = Propagation.MANDATORY)
	public void lockForUpdate(long fileHouseId, @NotNull LockService.LockCallback lockCallback) {
		fileHouseService.findOneByFilter(new Filter("id", Filter.Operator.eq, fileHouseId), true);
		lockCallback.afterLocked();
//		String key = mosServerProperties.getRedisPrefix() + "fileHouse-" + fileHouseId;
//		lockService.doWithLock(key, LockService.LockType.WRITE, lockCallback);
	}
	
	@Transactional(propagation = Propagation.MANDATORY)
	public void lockForRead(long fileHouseId) {
		jdbcTemplate.queryForList("select 0 from mos_file_house where id = ? lock in share mode", fileHouseId);
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
