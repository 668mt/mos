package mt.common.mybatis.event;

import org.springframework.context.ApplicationEvent;

/**
 * 初始化事件
* @ClassName: InitEvent
* @Description: 
* @author Martin
* @date 2017-10-24 上午11:28:59
*
 */
public class BeforeInitEvent extends ApplicationEvent{

	private static final long serialVersionUID = 4695186067723946564L;
	
	public BeforeInitEvent(Object source) {
		super(source);
	}

}
