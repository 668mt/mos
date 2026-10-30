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
	/**
	 * 显式指定生成 URL 用的地址（如当前请求的域名）。
	 * 填了就用它，不再按 {@link #forInner} / {@link #forExtern} 选择。
	 * 一般不用填：内网/外网地址由 {@code MosConfig.hosts} 里的配置决定
	 * （内网地址用 {@code http://ip:port}，外网地址写在 {@code []} 中）。
	 */
	private String host;
	/**
	 * 生成<b>内网</b>地址的 URL：给内部调用/同内网互访用。
	 * <p>与 {@link #forExtern} 互斥，两个都为 true 时以本项为准。</p>
	 */
	private Boolean forInner;
	/**
	 * 生成<b>外网</b>地址的 URL：给最终用户（外网）用。
	 * <p>不设时默认就是外网地址。</p>
	 */
	private Boolean forExtern;
	private String pathname;
	private Boolean render;
	private Boolean gallery;
	private Long expiredTime;
	private TimeUnit expiredTimeUnit;
	private String sign;
	/**
	 * 是否生成短链接；为 true 时会调用 mos-server 的 {@code POST /s}（body 传 bucketName/pathname/sign/expireSeconds/queryString），
	 * 返回 shortCode 后拼成 {@code {外网地址}/s/{shortCode}}。queryString 字段会在创建时被存入 Redis，
	 * 访问短链时由后端 302 跳转透传到 /mos/{bucketName}{pathname}。
	 */
	private Boolean useShortUrl;
	
	public static UrlBuildParamsBuilder builder(@NotNull String pathname, @NotNull Long expiredTime, @NotNull TimeUnit expiredTimeUnit) {
		return new UrlBuildParamsBuilder().pathname(pathname).expiredTime(expiredTime).expiredTimeUnit(expiredTimeUnit);
	}
}
