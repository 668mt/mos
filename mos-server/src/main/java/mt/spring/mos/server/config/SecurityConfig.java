package mt.spring.mos.server.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mt.common.entity.ResResult;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.service.UserService;
import mt.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.sql.DataSource;
import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	@Autowired
	private DataSource dataSource;
	private final static String REMEMBER_KEY = "4c53f5d4-bda9-4789-85db-dde405aa9d9c";
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean
	public MyRememberMeService myRememberMeServices(UserService userService, RedisTemplate<String, Object> redisTemplate) {
		return new MyRememberMeService(redisTemplate, REMEMBER_KEY, userService, persistentTokenRepository());
	}
	
	@Bean
	public SecurityFilterChain securityFilterChain(MyRememberMeService myRememberMeService, UserService userService, HttpSecurity http) throws Exception {
		http.formLogin()
			.loginPage("/signin")
			.loginProcessingUrl("/login")
			.successHandler(new MySuccessHandler(userService))
			.failureHandler(new MyFailHandler(userService))
//				.defaultSuccessUrl("/list")
			.and()
			.authorizeHttpRequests()
			.requestMatchers("/eureka/**").permitAll()
			.requestMatchers("/mos/**", "/render/**", "/gallary/**").permitAll()
			.requestMatchers("/signin/**").permitAll()
			.requestMatchers("/crossdomain.xml").permitAll()
			.requestMatchers("/member/bucket/grant/perms/own").permitAll()
			.requestMatchers("/css/**", "/js/**", "/img/**", "/ckplayer/**", "/iconfont/**", "/layui/**", "/index.html").permitAll()
			.requestMatchers("/upload/**").permitAll()
			.requestMatchers("/open/**").permitAll()
			.requestMatchers("/list/**").permitAll()
			.requestMatchers("/discovery/**").permitAll()
			.requestMatchers("/kaptcha/**").permitAll()
			.requestMatchers("/favicon.ico").permitAll()
			.requestMatchers("/admin/**").hasRole("ADMIN")
			.requestMatchers("/member/**").hasAnyRole("ADMIN", "MEMBER")
			.requestMatchers("/test/**").permitAll()
			.requestMatchers("/health").permitAll()
			.requestMatchers("/actuator/**").permitAll()
			.anyRequest().authenticated()
			.and().rememberMe()
			.rememberMeParameter("rememberMe")
			.key(REMEMBER_KEY)
			.rememberMeServices(myRememberMeService)
			.tokenValiditySeconds(3600 * 24 * 30)
			.userDetailsService(userService)
			.and().csrf().disable()
			.headers().cacheControl().disable()
//				.and().cors(withDefaults())
			.and().cors().disable()
		;
		return http.build();
	}
	
	@Bean
	public HttpFirewall allowUrlSemicolonHttpFirewall() {
		StrictHttpFirewall firewall = new StrictHttpFirewall();
		firewall.setAllowSemicolon(true);
		firewall.setAllowUrlEncodedDoubleSlash(true);
		firewall.setAllowUrlEncodedPercent(true);
		firewall.setAllowUrlEncodedSlash(true);
		firewall.setAllowBackSlash(true);
		firewall.setAllowUrlEncodedLineFeed(true);
		firewall.setAllowUrlEncodedCarriageReturn(true);
		firewall.setAllowUrlEncodedPeriod(true);
		firewall.setAllowUrlEncodedParagraphSeparator(true);
		firewall.setAllowUrlEncodedLineSeparator(true);
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
		private final UserService userService;
		
		public MySuccessHandler(UserService userService) {
			this.userService = userService;
		}
		
		@Override
		public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
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
		private final UserService userService;
		
		public MyFailHandler(UserService userService) {
			this.userService = userService;
		}
		
		@Override
		public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
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