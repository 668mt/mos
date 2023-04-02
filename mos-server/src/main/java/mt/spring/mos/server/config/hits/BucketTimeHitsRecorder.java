package mt.spring.mos.server.config.hits;

import mt.common.hits.HitsRecorder;

import java.util.List;

/**
 * @Author Martin
 * @Date 2023/4/2
 */
public interface BucketTimeHitsRecorder extends HitsRecorder<String, Long> {
	String getKey(long bucketId);
	
	List<TimeHits> getEffectData(long bucketId);
	
	long getTotal(long bucketId);
}
