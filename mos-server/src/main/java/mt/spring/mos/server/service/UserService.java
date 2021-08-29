package mt.spring.mos.server.service;

import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.entity.dto.UserAddDTO;
import mt.spring.mos.server.entity.dto.UserUpdateDTO;
import mt.spring.mos.server.entity.po.User;
import mt.spring.mos.server.entity.vo.BucketVo;
import mt.utils.common.Assert;
import mt.utils.common.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2020/5/25
 */
@Service
public class UserService extends BaseServiceImpl<User> implements UserDetailsService {
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	@Lazy
	private BucketService bucketService;
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	
	@Transactional
	public User addUser(UserAddDTO userAddDTO) {
		Assert.state(!exists("username", userAddDTO.getUsername()), "用户名已存在");
		User user = BeanUtils.transformOf(userAddDTO, User.class);
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		save(user);
		return user;
	}
	
	@Transactional
	public User updateUser(UserUpdateDTO userUpdateDTO) {
		if (StringUtils.isNotBlank(userUpdateDTO.getUsername())) {
			List<Filter> filters = new ArrayList<>();
			filters.add(new Filter("username", Filter.Operator.eq, userUpdateDTO.getUsername()));
			filters.add(new Filter("id", Filter.Operator.ne, userUpdateDTO.getId()));
			User oneByFilter = findOneByFilters(filters);
			Assert.state(oneByFilter == null, "用户名已存在");
		}
		User user = BeanUtils.transformOf(userUpdateDTO, User.class);
		if (StringUtils.isNotBlank(user.getPassword())) {
			user.setPassword(passwordEncoder.encode(user.getPassword()));
		} else {
			user.setPassword(null);
		}
		updateByIdSelective(user);
		return user;
	}
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		if (isNeedCode(username)) {
			ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
			HttpServletRequest request = Objects.requireNonNull(servletRequestAttributes).getRequest();
			HttpSession session = request.getSession();
			String code = request.getParameter("code");
			Object kaptchaCode = session.getAttribute("kaptchaCode");
			session.removeAttribute("kaptchaCode");
			Assert.state(code != null && code.equalsIgnoreCase(kaptchaCode + ""), "验证码不正确");
		}
		User user = findOne("username", username);
		if (user == null) {
			throw new UsernameNotFoundException("用户不存在");
		}
		return user;
	}
	
	@Override
	public boolean existsId(Object record) {
		return super.existsId(record);
	}
	
	@Override
	public User findById(Object record) {
		return super.findById(record);
	}
	
	@Transactional
	public int deleteUser(Long userId) {
		//删除用户的所有bucket
		List<BucketVo> list = bucketService.findBucketList(userId).stream().filter(BucketVo::getIsOwn).collect(Collectors.toList());
		for (BucketVo bucketVo : list) {
			bucketService.deleteBucket(bucketVo.getId(), userId);
		}
		//删除用户
		return deleteById(userId);
	}
	
	@Transactional
	public void unlock(String username) {
		String key = "failTimes:" + username;
		redisTemplate.delete(key);
		User user = findOne("username", username);
		if (user != null) {
			user.setFailures(0);
			user.setLocked(false);
			updateById(user);
		}
	}
	
	@Transactional
	public void addLoginFailTimes(String username) {
		String key = "failTimes:" + username;
		Integer failTimes = (Integer) redisTemplate.opsForValue().get(key);
		if (failTimes == null) {
			failTimes = 0;
		}
		redisTemplate.opsForValue().set(key, ++failTimes, Duration.ofDays(1));
		User user = findOne("username", username);
		if (user != null) {
			user.setFailures(user.getFailures() + 1);
			if (user.getFailures() >= 5) {
				user.setLocked(true);
			}
			updateById(user);
		}
	}
	
	public boolean isNeedCode(String username) {
		String key = "failTimes:" + username;
		Integer failTimes = (Integer) redisTemplate.opsForValue().get(key);
		return failTimes != null && failTimes >= 3;
	}
	
	@Transactional
	public void onLoginSuccess(String username) {
		User user = findOne("username", username);
		user.setLocked(false);
		user.setLastLoginDate(new Date());
		Long loginTimes = user.getLoginTimes();
		user.setLoginTimes(++loginTimes);
		updateById(user);
		unlock(username);
	}
}
