//package mt.utils.cache;
//
//import mt.utils.SpringUtils;
//import mt.utils.redis.MyRedisCacheManager;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//import org.springframework.cache.Cache;
//import org.springframework.data.redis.cache.RedisCache;
//
///**
// * @Author Martin
// * @Date 2018/5/31
// */
//public final class RedisCacheUtils {
//
//	private RedisCacheUtils() {
//	}
//
//	private static MyRedisCacheManager redisCacheManager = SpringUtils.getBean(MyRedisCacheManager.class);
////	private static RedisCacheManager redisCacheManager = SpringUtils.getBean(RedisCacheManager.class);
//
//	public interface DoCache{
//		Object handle();
//	}
//
//	/**
//	 * 缓存，如果存在返回缓存，不存在创建缓存
//	 * @param name
//	 * @param key
//	 * @param doCache
//	 * @return
//	 */
//	public static Object cachable(String name, String key, DoCache doCache){
//		return cachable(name,key,doCache,null);
//	}
//
//	public static Object cachable(String name, String key, DoCache doCache, Long expire){
//		Object value = getValue(name, key);
//		if(value != null){
//			return value;
//		}else{
//			value = doCache.handle();
//			if(expire == null){
//				addElement(name,key,value);
//			}else {
//				addElement(name, key, value, expire);
//			}
//		}
//		return value;
//	}
//
//	public static Cache getCache(String name){
//		return redisCacheManager.getCacheIfExists(name);
//	}
//
//	public static void removeKey(String name, Object key){
//		Cache cache = getCache(name);
//		if(cache != null) {
//			cache.evict(key);
//		}
//	}
//	public static void removeAll(String name){
//		Cache cache = getCache(name);
//		if(cache != null) {
//			cache.clear();
//		}
//	}
//
//	public static void addElement(@NotNull String name, @NotNull Object key, Object value){
//		Cache cache = getCache(name);
//		if(cache == null) {
//			cache = addCache(name);
//		}
//		cache.put(key, value);
//	}
//	public static void addElement(@NotNull String name, @NotNull Object key, Object value, long expire){
//		Cache cache1 = addCacheIfAbsent(name, expire);
//		cache1.put(key,value);
//	}
//
//	/**
//	 * 获取缓存值
//	 *
//	 * @param name
//	 * @param key
//	 * @return
//	 */
//	@Nullable
//	public static Object getValue(@NotNull String name, @NotNull Object key) {
//		Cache cache = getCache(name);
//		if (cache == null) {
//			return null;
//		}
//		Cache.ValueWrapper valueWrapper = cache.get(key);
//		if(valueWrapper == null) return null;
//		return valueWrapper.get();
//	}
//
//	/**
//	 * 如果缓存不存在则新增缓存
//	 * @param name 缓存名
//	 * @param expire 有效时间
//	 * @return
//	 */
//	public static Cache addCacheIfAbsent(@NotNull String name, long expire) {
//		RedisCache cache = (RedisCache) redisCacheManager.addCacheIfAbsent(name,expire);
//		return cache;
//	}
//	public static Cache addCache(@NotNull String name){
//		return redisCacheManager.addCache(name);
//	}
//	public static Cache addCache(@NotNull String name, long expire){
//		return redisCacheManager.addCache(name,expire);
//	}
//}
