package mt.spring.mos.server.service;

import mt.common.service.DataLockService;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2020/12/17
 */
@Service
public class LockService {
	@Autowired
	private RedissonClient redissonClient;

	public enum LockType {
		READ, WRITE
	}

	public interface LockCallbackWithResult<T> {
		T afterLocked();
	}

	public interface LockCallback {
		void afterLocked();
	}

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private DataLockService dataLockService;

//	@Transactional(rollbackFor = Exception.class)
//	public <T> T doWithLock(String key, LockType lockType, int lockMinutes, LockCallbackWithResult<T> lockCallbackWithResult) {
////		dataLockService.lock("sn", jdbcTemplate);
////		return lockCallbackWithResult.afterLocked();
//		RLock lock = null;
//		try {
//			RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(key);
//			lock = lockType == LockType.READ ? readWriteLock.readLock() : readWriteLock.writeLock();
//			lock.lock(lockMinutes, TimeUnit.MINUTES);
//			return lockCallbackWithResult.afterLocked();
//		} finally {
//			if (lock != null) {
//				lock.unlock();
//			}
//		}
//	}

	public boolean tryLock(String key, LockCallback lockCallback) {
		RLock lock = redissonClient.getLock(key);
		if (lock.tryLock()) {
			try {
				lockCallback.afterLocked();
				return true;
			} finally {
				lock.unlock();
			}
		} else {
			return false;
		}
	}

}
