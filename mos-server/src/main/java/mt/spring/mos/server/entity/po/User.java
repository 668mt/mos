package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.spring.mos.server.entity.BaseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Collection;
import java.util.List;

/**
 * @Author Martin
 * @Date 2020/5/23
 */
@Table(name = "mos_user")
@Data
@EqualsAndHashCode(callSuper = false)
public class User extends BaseEntity implements UserDetails {
	private static final long serialVersionUID = -5776046036024197542L;
	@Id
	@KeySql(useGeneratedKeys = true)
	private Long id;
	@Column(unique = true)
	private String username;
	private String password;
	private Boolean isEnable;
	private Boolean isAdmin;
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		List<GrantedAuthority> grantedAuthorities = new java.util.ArrayList<>();
		if (isAdmin != null && isAdmin) {
			grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
		} else {
			grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_MEMBER"));
		}
		return grantedAuthorities;
	}
	
	@Override
	public boolean isAccountNonExpired() {
		return true;
	}
	
	@Override
	public boolean isAccountNonLocked() {
		return true;
	}
	
	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}
	
	@Override
	public boolean isEnabled() {
		return isEnable == null ? false : isEnable;
	}
}
