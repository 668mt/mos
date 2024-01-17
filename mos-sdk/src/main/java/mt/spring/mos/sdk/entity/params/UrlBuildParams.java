package mt.spring.mos.sdk.entity.params;

import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2024/1/17
 */
@Data
@Builder
public class UrlBuildParams {
	private String host;
	private String pathname;
	private Boolean render;
	private Boolean gallery;
	private Long expiredTime;
	private TimeUnit expiredTimeUnit;
	private String sign;
	
	public static UrlBuildParamsBuilder builder(@NotNull String pathname, @NotNull Long expiredTime, @NotNull TimeUnit expiredTimeUnit) {
		return new UrlBuildParamsBuilder().pathname(pathname).expiredTime(expiredTime).expiredTimeUnit(expiredTimeUnit);
	}
}
