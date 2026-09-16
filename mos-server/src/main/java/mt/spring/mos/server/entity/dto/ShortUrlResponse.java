package mt.spring.mos.server.entity.dto;

import lombok.Data;

/**
 * 创建短链接接口的响应 DTO
 */
@Data
public class ShortUrlResponse {
	private String shortCode;
}
