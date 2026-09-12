package mt.spring.mos.server.entity.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.common.starter.message.annotation.BatchMessage;
import mt.spring.mos.server.entity.handler.ResourceSignUrlBatchMessageHandler;
import mt.spring.mos.server.entity.po.Resource;

/**
 * @Author Martin
 * @Date 2026/9/12
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class ResourceVo extends Resource {
	@BatchMessage(column = "id", handlerClass = ResourceSignUrlBatchMessageHandler.class)
	private String signUrl;
}
