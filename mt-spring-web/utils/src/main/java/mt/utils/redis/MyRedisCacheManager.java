//package mt.utils.redis;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cache.Cache;
//import org.springframework.data.redis.cache.RedisCache;
//import org.springframework.data.redis.cache.RedisCacheManager;
//import org.springframework.data.redis.core.RedisOperations;
//
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * @Author Martin
// * @Date 2018/6/9
// */
//@Slf4j
//public class MyRedisCacheManager extends RedisCacheManager {
//
//	private Map<String, Long> expires = new HashMap<>();
//
//	public MyRedisCacheManager(RedisOperations redisOperations) {
//		super(redisOperations);
//	}
//
//	public MyRedisCacheManager(RedisOperations redisOperations, Collection<String> cacheNames) {
//		super(redisOperations, cacheNames);
//	}
//
//	public MyRedisCacheManager(RedisOperations redisOperations, Collection<String> cacheNames, boolean cacheNullValues) {
//		super(redisOperations, cacheNames, cacheNullValues);
//	}
//
//	public void addExpire(String name, long expire) {
//		expires.put(name, expire);
//	}
//
//	@Override
//	protected long computeExpiration(String name) {
//		Long expiration = expires.get(name);
//		if (expiration != null) {
//			return expiration;
//		}
//		return super.computeExpiration(name);
//	}
//
//	/**
//	 * 获取缓存，如果没有，返回null
//	 *
//	 * @param name
//	 * @return
//	 */
//	public Cache getCacheIfExists(String name) {
//		Collection<String> cacheNames = super.getCacheNames();
//		if (cacheNames.contains(name)) {
//			return super.getCache(name);
//		}
//		return null;
//	}
//
//	public Cache addCache(String name) {
//		return addCache(name, null);
//	}
//
//	/**
//	 * 新增缓存
//	 *
//	 * @param name
//	 * @param expire
//	 * @return
//	 */
//	public Cache addCache(String name, Long expire) {
//		if (expire != null) {
//			expires.put(name, expire);
//		}
//		RedisCache cache = super.createCache(name);
//		super.addCache(cache);
//		return cache;
//	}
//	public Cache addCacheIfAbsent(String name, Long expire) {
//		if (expire != null) {
//			expires.put(name, expire);
//		}
//		if(!super.getCacheNames().contains(name)){
//			RedisCache cache = super.createCache(name);
//			super.addCache(cache);
//			return cache;
//		}
//		return super.getCache(name);
//	}
//}
