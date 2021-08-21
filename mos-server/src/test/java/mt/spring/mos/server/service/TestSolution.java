package mt.spring.mos.server.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author Martin
 * @Date 2021/6/8
 */
@Slf4j
public class TestSolution {
	
	public static void main(String[] args) {
		
	}
	
//	public static void main(String[] args) {
////		设计一个LRU 数据结构：
////		首先要接收一个 capacity 参数作为缓存的最大容量，然后实现两个 API：
////		- 一个是 put(key, val) 方法存入键值对
////				- 另一个是 get(key) 方法获取 key 对应的 val，如果 key 不存在则返回 -1
////		限制：get 和 put 方法必须都是 O(1) 的时间复杂度
//		LRUCache lruCache = new LRUCache(5);
//		lruCache.put("test", 11);
//		lruCache.put("test2", 22);
//		lruCache.put("test3", 33);
//		lruCache.put("test4", 44);
//		lruCache.put("test5", 55);
//		System.out.println(lruCache.get("test"));
//		System.out.println(lruCache.get("test2"));
////		System.out.println(lruCache.get("test3"));
//		System.out.println(lruCache.get("test4"));
//		System.out.println(lruCache.get("test5"));
//		lruCache.put("test6", 66);
//
//		Map<String, LRUValue> caches = lruCache.getCaches();
//		for (Map.Entry<String, LRUValue> stringLRUValueEntry : caches.entrySet()) {
//			LRUValue value = stringLRUValueEntry.getValue();
//			log.info("{}={},hits={}", stringLRUValueEntry.getKey(), value.getValue(), value.getHits());
//		}
//	}
	
	public static class LRUCache {
		private final Map<String, LRUValue> caches;
		private final int capacity;
		
		public Map<String, LRUValue> getCaches() {
			return caches;
		}
		
		public LRUCache(int capacity) {
			this.capacity = capacity;
			caches = new HashMap<>(capacity);
		}
		
		public synchronized void put(String key, Object value) {
			if (caches.size() >= capacity) {
				//需要进行LRU计算
				removeLowestHitValue();
			}
			LRUValue lruValue = new LRUValue();
			lruValue.setValue(value);
			caches.put(key, lruValue);
		}
		
		private void removeLowestHitValue() {
			String key = null;
			int minHists = Integer.MAX_VALUE;
			for (Map.Entry<String, LRUValue> stringLRUValueEntry : caches.entrySet()) {
				LRUValue value = stringLRUValueEntry.getValue();
				if (value.getHits().get() < minHists) {
					minHists = value.getHits().get();
					key = stringLRUValueEntry.getKey();
				}
			}
			if (key != null) {
				caches.remove(key);
			}
		}
		
		public Object get(String key) {
			LRUValue lruValue = caches.get(key);
			if (lruValue == null) {
				return -1;
			} else {
				lruValue.getHits().getAndIncrement();
				return lruValue;
			}
		}
	}
	
	@Data
	public static class LRUValue {
		private Object value;
		private AtomicInteger hits = new AtomicInteger(0);
	}
	
}
