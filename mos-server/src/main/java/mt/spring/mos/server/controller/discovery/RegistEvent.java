package mt.spring.mos.server.controller.discovery;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * @Author Martin
 * @Date 2020/6/7
 */
public class RegistEvent extends ApplicationEvent {
	private static final long serialVersionUID = -8851515234364259625L;
	@Getter
	@Setter
	private Instance instance;
	
	public RegistEvent(Object source, Instance instance) {
		super(source);
		this.instance = instance;
	}
}
