package mt.spring.mos.server.service;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
		T afterLocked() throws Exception;
	}
	
	public interface LockCallback {
		void afterLocked() throws Exception;
	}
	
	@SneakyThrows
	public void doWithLock(@Nullable String key, @Nullable LockType lockType, @NotNull LockCallback lockCallback) {
		if (StringUtils.isBlank(key)) {
			lockCallback.afterLocked();
			return;
		}
		RLock lock = null;
		try {
			RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(key);
			lock = lockType == LockType.READ ? readWriteLock.readLock() : readWriteLock.writeLock();
			lock.lock();
			lockCallback.afterLocked();
		} finally {
			if (lock != null) {
				lock.unlock();
			}
		}
	}
	
	@SneakyThrows
	public <T> T doWithLockResult(@Nullable String key, @Nullable LockType lockType, @NotNull LockCallbackWithResult<T> lockCallback) {
		if (StringUtils.isBlank(key)) {
			return lockCallback.afterLocked();
		}
		RLock lock = null;
		try {
			RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(key);
			lock = lockType == LockType.READ ? readWriteLock.readLock() : readWriteLock.writeLock();
			lock.lock();
			return lockCallback.afterLocked();
		} finally {
			if (lock != null) {
				lock.unlock();
			}
		}
	}
	
	@SneakyThrows
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
	
	@SneakyThrows
	public <T> T tryWithLockResult(String key, LockCallbackWithResult<T> lockCallback) {
		RLock lock = redissonClient.getLock(key);
		if (lock.tryLock()) {
			try {
				return lockCallback.afterLocked();
			} finally {
				lock.unlock();
			}
		} else {
			return null;
		}
	}
	
}
