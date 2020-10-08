package mt.spring.mos.server.entity.messagehandler;

import mt.common.starter.message.messagehandler.AbstractCacheMessageHandler;
import mt.spring.mos.server.entity.po.User;
import mt.spring.mos.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author Martin
 * @Date 2020/10/8
 */
@Component
public class UserId2UsernameHandler extends AbstractCacheMessageHandler<String> {
	@Autowired
	private UserService userService;
	
	@Override
	public String getCacheKey(Object[] params, String mark) {
		return getParam(params, 0, Long.class) + "";
	}
	
	@Override
	public String getNoCacheValue(Object[] params, String mark) {
		Long userId = getParam(params, 0, Long.class);
		User user = userService.findById(userId);
		if (user != null) {
			return user.getUsername();
		}
		return null;
	}
}
