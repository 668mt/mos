package mt.common.starter.message.messagehandler;

/**
 * @Author Martin
 * @Date 2019/1/6
 */
public class DefaultMessageHandler implements MessageHandler {
	@Override
	public Object handle(Object[] params, String mark) {
		if (params == null)
			return null;
		return params[0];
	}
}
