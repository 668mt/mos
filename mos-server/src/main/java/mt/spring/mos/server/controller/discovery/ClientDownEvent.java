package mt.spring.mos.server.controller.discovery;

import lombok.Getter;
import lombok.Setter;
import mt.spring.mos.server.entity.po.Client;
import org.springframework.context.ApplicationEvent;

/**
 * @Author Martin
 * @Date 2020/6/7
 */
public class ClientDownEvent extends ApplicationEvent {
	private static final long serialVersionUID = -8851515234364259625L;
	@Getter
	@Setter
	private Client client;
	
	public ClientDownEvent(Object source, Client client) {
		super(source);
		this.client = client;
	}
}
