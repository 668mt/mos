package mt.spring.mos.server.entity.dto;

import lombok.Data;

/**
 * 创建短链接接口的请求 body
 *
 * 注意：{@code bucketName} / {@code sign} MUST 通过 query/form 参数传入（与项目内其它 OpenApi 接口一致），
 * 以便 {@code OpenApiAspect} 直接从 method args / {@code request.getParameter} 拿到，避免走反射遍历 arg 的兜底分支。
 */
@Data
public class ShortUrlCreateRequest {
	private long expireSeconds;
	private String queryString;
}
