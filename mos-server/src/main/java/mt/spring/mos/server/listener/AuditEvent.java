package mt.spring.mos.server.listener;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import mt.spring.mos.server.entity.po.Audit;
import org.springframework.context.ApplicationEvent;

/**
 * @Author Martin
 * @Date 2020/12/19
 */
@Getter
@Setter
public class AuditEvent extends ApplicationEvent {
	/**
	 * Create a new {@code ApplicationEvent}.
	 *
	 * @param source the object on which the event initially occurred or with
	 *               which the event is associated (never {@code null})
	 */
	public AuditEvent(Object source) {
		super(source);
	}
	
	private Audit audit;
	
}
