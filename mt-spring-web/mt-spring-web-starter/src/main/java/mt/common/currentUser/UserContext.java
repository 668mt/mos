package mt.common.currentUser;

/**
 * @Author Martin
 * @Date 2019/8/24
 */
public interface UserContext {
	Object getCurrentUser();
	
	Object getCurrentUserId();
}
