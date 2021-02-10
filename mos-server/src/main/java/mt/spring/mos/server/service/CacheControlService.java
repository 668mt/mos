package mt.spring.mos.server.service;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2021/2/10
 */
@Service
public class CacheControlService {
	@Autowired
	private StringRedisTemplate stringRedisTemplate;
	
	public void setNoCache(long resourceId) {
		stringRedisTemplate.opsForValue().set(getKey(resourceId, true), "clearCache", 1, TimeUnit.HOURS);
		stringRedisTemplate.opsForValue().set(getKey(resourceId, false), "clearCache", 1, TimeUnit.HOURS);
	}
	
	private String getKey(long resourceId, boolean thumb) {
		return "refresh-content-type:" + resourceId + ":" + thumb;
	}
	
	public boolean needNoCache(long resourceId, boolean thumb) {
		String key = getKey(resourceId, thumb);
		String s = stringRedisTemplate.opsForValue().get(key);
		return StringUtils.isNotBlank(s);
	}
	
	public void clearNoCache(long resourceId, boolean thumb) {
		stringRedisTemplate.delete(getKey(resourceId, thumb));
	}
}
