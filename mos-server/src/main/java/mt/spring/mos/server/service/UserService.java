package mt.spring.mos.server.service;

import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.dao.UserMapper;
import mt.spring.mos.server.entity.dto.UserAddDTO;
import mt.spring.mos.server.entity.dto.UserUpdateDTO;
import mt.spring.mos.server.entity.po.User;
import mt.utils.Assert;
import mt.utils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Martin
 * @Date 2020/5/25
 */
@Service
public class UserService extends BaseServiceImpl<User> implements UserDetailsService {
	@Autowired
	private UserMapper userMapper;
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Override
	public BaseMapper<User> getBaseMapper() {
		return userMapper;
	}
	
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
		}
		updateByIdSelective(user);
		return user;
	}
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = findOne("username", username);
		if (user == null) {
			throw new UsernameNotFoundException("用户不存在");
		}
		return user;
	}
	
	@Override
	@Cacheable("userCache")
	public boolean existsId(Object record) {
		return super.existsId(record);
	}
}
