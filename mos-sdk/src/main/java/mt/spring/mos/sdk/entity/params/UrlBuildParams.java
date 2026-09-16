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
	/**
	 * 是否生成短链接；为 true 时会调用 mos-server 的 {@code POST /s}（body 传 bucketName/pathname/sign/expireSeconds/queryString），
	 * 返回 shortCode 后拼成 {@code {host}/s/{shortCode}}。queryString 字段会在创建时被存入 Redis，
	 * 访问短链时由后端 302 跳转透传到 /mos/{bucketName}{pathname}。
	 */
	private Boolean useShortUrl;
	
	public static UrlBuildParamsBuilder builder(@NotNull String pathname, @NotNull Long expiredTime, @NotNull TimeUnit expiredTimeUnit) {
		return new UrlBuildParamsBuilder().pathname(pathname).expiredTime(expiredTime).expiredTimeUnit(expiredTimeUnit);
	}
}
