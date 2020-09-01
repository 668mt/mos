package mt.spring.mos.server.controller.discovery;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * @Author Martin
 * @Date 2020/6/7
 */
public class BeatEvent extends ApplicationEvent {
	private static final long serialVersionUID = 6831620411716508640L;
	@Getter
	@Setter
	private Instance instance;
	
	public BeatEvent(Object source, Instance instance) {
		super(source);
		this.instance = instance;
	}
}
