package mt.spring.mos.server.config.hits;

import lombok.Getter;
import mt.common.hits.LocalHitsRecorder;
import org.jetbrains.annotations.NotNull;

/**
 * @Author Martin
 * @Date 2023/4/2
 */
public class MosHitsRecorder extends LocalHitsRecorder<Long, String> {
	@Getter
	private final ResourceRedisTimeDownHandler hitsDownHandler;
	
	public MosHitsRecorder(@NotNull ResourceRedisTimeDownHandler hitsDownHandler) {
		super(hitsDownHandler);
		this.hitsDownHandler = hitsDownHandler;
	}
}
