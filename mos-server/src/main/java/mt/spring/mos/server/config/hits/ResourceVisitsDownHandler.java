package mt.spring.mos.server.config.hits;

import mt.common.hits.HitsDownHandler;
import mt.spring.mos.server.dao.ResourceMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @Author Martin
 * @Date 2023/4/2
 */
@Component
public class ResourceVisitsDownHandler implements HitsDownHandler<String, Long> {
	@Autowired
	private ResourceMapper resourceMapper;
	
	@Override
	public void doHitsDown(@Nullable String scope, @NotNull Map<Long, Long> hitsMap) {
		for (Map.Entry<Long, Long> longLongEntry : hitsMap.entrySet()) {
			Long resourceId = longLongEntry.getKey();
			Long hits = longLongEntry.getValue();
			resourceMapper.addVisits(resourceId, hits);
		}
	}
}
