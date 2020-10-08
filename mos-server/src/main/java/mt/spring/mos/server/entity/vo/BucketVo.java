package mt.spring.mos.server.entity.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.common.starter.message.annotation.Message;
import mt.spring.mos.server.entity.messagehandler.UserId2UsernameHandler;
import mt.spring.mos.server.entity.po.Bucket;

import javax.persistence.Transient;

/**
 * @Author Martin
 * @Date 2020/10/8
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class BucketVo extends Bucket {
	@Transient
	private Boolean isOwn;
	
	@Message(params = "#userId", handlerClass = UserId2UsernameHandler.class)
	private String owner;
}
