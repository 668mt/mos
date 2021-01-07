package mt.spring.mos.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/**
 * @Author Martin
 * @Date 2021/1/7
 */
@Service
@Slf4j
public class CacheService {
	
	@CacheEvict(value = {"permCache", "bucketCache", "accessControlCache"}, allEntries = true)
	public void clearAll() {
		log.info("清除所有缓存成功");
	}
}
