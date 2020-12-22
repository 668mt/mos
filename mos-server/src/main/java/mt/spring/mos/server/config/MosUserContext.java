package mt.spring.mos.server.config;

import lombok.extern.slf4j.Slf4j;
import mt.common.currentUser.UserContext;
import mt.spring.mos.server.entity.po.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * @Author Martin
 * @Date 2020/5/23
 */
@Component
@Slf4j
public class MosUserContext implements UserContext<User, Long> {
	
	@Override
	public User getCurrentUser() {
		try {
			SecurityContext context = SecurityContextHolder.getContext();
			Authentication authentication = context.getAuthentication();
			if (authentication == null) {
				return null;
			}
			Object principal = authentication.getPrincipal();
			if (principal instanceof User) {
				return (User) principal;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}
	
	@Override
	public Long getCurrentUserId() {
		User currentUser = getCurrentUser();
		if (currentUser != null) {
			return currentUser.getId();
		}
		return null;
	}
	
	@Override
	public String getCurrentUserName() {
		User currentUser = getCurrentUser();
		if (currentUser != null) {
			return currentUser.getUsername();
		}
		return null;
	}
}
