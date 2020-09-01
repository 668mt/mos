package mt.common.starter.message.messagehandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author LIMAOTAO236
 * @Date 2020-04-30
 */
public abstract class AbstractCacheMessageHandler<T> implements MessageHandler {
	protected Map<String, T> cache = new HashMap<>();
	
	public void clearCache() {
		cache.clear();
	}
	
	@Override
	public void init() {
		clearCache();
	}
	
	@Override
	public Object handle(Object[] params, String mark) {
		return getValueUseCacheOrNoCache(params, mark);
	}
	
	public abstract String getCacheKey(Object[] params, String mark);
	
	public abstract T getNoCacheValue(Object[] params, String mark);
	
	public T getValueUseCacheOrNoCache(Object[] params, String mark) {
		String cacheKey = getCacheKey(params, mark);
		T o = cache.get(cacheKey);
		if (o != null) {
			return o;
		} else {
			o = getNoCacheValue(params, mark);
			cache.put(cacheKey, o);
			return o;
		}
	}
	
}
