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
	/**
	 * 外网域名（如 CDN / 反代后对外暴露的域名）。与 {@link #host} 并存：
	 * <ul>
	 *     <li>{@code host} 用于调用 mos-server（如签名、生成短链接的 POST /s），一般是内网地址</li>
	 *     <li>{@code domain} 用于拼最终面向外网分享的 URL（短链、长链），为空时回退到 {@code host}</li>
	 * </ul>
	 */
	private String domain;
	private String pathname;
	private Boolean render;
	private Boolean gallery;
	private Long expiredTime;
	private TimeUnit expiredTimeUnit;
	private String sign;
	/**
	 * 是否生成短链接；为 true 时会调用 mos-server 的 {@code POST /s}（body 传 bucketName/pathname/sign/expireSeconds/queryString），
	 * 返回 shortCode 后拼成 {@code {domain}/s/{shortCode}}（domain 为空时回退到 host）。queryString 字段会在创建时被存入 Redis，
	 * 访问短链时由后端 302 跳转透传到 /mos/{bucketName}{pathname}。
	 */
	private Boolean useShortUrl;
	
	public static UrlBuildParamsBuilder builder(@NotNull String pathname, @NotNull Long expiredTime, @NotNull TimeUnit expiredTimeUnit) {
		return new UrlBuildParamsBuilder().pathname(pathname).expiredTime(expiredTime).expiredTimeUnit(expiredTimeUnit);
	}
}
