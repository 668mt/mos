package mt.spring.mos.server.config;

import mt.common.entity.ResResult;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.service.UserService;
import mt.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	@Autowired
	private UserService userService;
	@Autowired
	private DataSource dataSource;
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		super.configure(auth);
	}
	
	@Bean
	public HttpFirewall allowUrlSemicolonHttpFirewall() {
		StrictHttpFirewall firewall = new StrictHttpFirewall();
		firewall.setAllowSemicolon(true);
		firewall.setAllowUrlEncodedDoubleSlash(true);
		firewall.setAllowUrlEncodedPercent(true);
		firewall.setAllowUrlEncodedDoubleSlash(true);
		firewall.setAllowUrlEncodedSlash(true);
		firewall.setAllowBackSlash(true);
		return firewall;
	}
	
	@Bean
	public PersistentTokenRepository persistentTokenRepository() {
		JdbcTokenRepositoryImpl jdbcTokenRepository = new JdbcTokenRepositoryImpl();
		jdbcTokenRepository.setDataSource(dataSource);
		// 自动建表，不建议开启
//		 jdbcTokenRepository.setCreateTableOnStartup(true);
		return jdbcTokenRepository;
	}
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.formLogin()
				.loginPage("/signin")
				.loginProcessingUrl("/login")
				.successHandler(new MySuccessHandler())
				.failureHandler(new MyFailHandler())
//				.defaultSuccessUrl("/list")
				.and().authorizeRequests()
				.antMatchers("/eureka/**").permitAll()
				.antMatchers("/mos/**", "/render/**", "/gallary/**").permitAll()
				.antMatchers("/signin/**").permitAll()
				.antMatchers("/member/bucket/grant/perms/own").permitAll()
				.antMatchers("/css/**", "/js/**", "/img/**", "/ckplayer/**", "/iconfont/**", "/layui/**", "/index.html").permitAll()
				.antMatchers("/upload/**").permitAll()
				.antMatchers("/open/**").permitAll()
				.antMatchers("/list/**").permitAll()
				.antMatchers("/discovery/**").permitAll()
				.antMatchers("/kaptcha/**").permitAll()
				.antMatchers("/favicon.ico").permitAll()
				.antMatchers("/admin/**").hasRole("ADMIN")
				.antMatchers("/member/**").hasAnyRole("ADMIN", "MEMBER")
				.antMatchers("/test/**").permitAll()
				.antMatchers("/health").permitAll()
				.antMatchers("/actuator/info").permitAll()
				.anyRequest().authenticated()
				.and().rememberMe()
				.rememberMeParameter("rememberMe")
				.tokenRepository(persistentTokenRepository())
				.tokenValiditySeconds(3600 * 24 * 30)
				.userDetailsService(userService)
				.and().csrf().disable()
				.headers().cacheControl().disable()
//				.and().cors(withDefaults())
				.and().cors().disable()
		;
	}
	
	@Bean
	CorsConfigurationSource corsConfigurationSource(MosServerProperties mosServerProperties) {
		MosServerProperties.CorsConfig corsConfig = mosServerProperties.getCorsConfig();
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(corsConfig.getAllowedOrigins());
		configuration.setAllowedMethods(corsConfig.getAllowedHeaders());
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
	
	public class MySuccessHandler implements AuthenticationSuccessHandler {
		@Override
		public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
			String username = request.getParameter("username");
			userService.onLoginSuccess(username);
			ResResult resResult = new ResResult();
			resResult.setStatus(ResResult.Status.ok);
			resResult.setMessage("登录成功!");
			resResult.setResult(authentication.getPrincipal());
			response.setContentType("application/json;charset=utf-8");
			response.getWriter().write(JsonUtils.toJson(resResult));
		}
	}
	
	public class MyFailHandler implements AuthenticationFailureHandler {
		@Override
		public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
			String username = request.getParameter("username");
			if (exception.getMessage().contains("用户名或密码错误")) {
				userService.addLoginFailTimes(username);
			}
			ResResult resResult = new ResResult();
			resResult.setStatus(ResResult.Status.error);
			resResult.setMessage(exception.getMessage());
			response.setContentType("application/json;charset=utf-8");
			response.getWriter().write(JsonUtils.toJson(resResult));
		}
	}
}