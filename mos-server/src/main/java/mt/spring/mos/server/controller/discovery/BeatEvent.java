package mt.spring.mos.server.controller.discovery;

import lombok.Getter;
import lombok.Setter;
import mt.spring.mos.server.entity.po.Client;
import org.springframework.context.ApplicationEvent;

/**
 * @Author Martin
 * @Date 2020/6/7
 */
public class BeatEvent extends ApplicationEvent {
	private static final long serialVersionUID = 6831620411716508640L;
	@Getter
	@Setter
	private Client client;
	
	public BeatEvent(Object source, Client client) {
		super(source);
		this.client = client;
	}
}
