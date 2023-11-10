package mt.spring.mos.server.config;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2023/5/3
 */
@Slf4j
public class MyRememberMeService extends PersistentTokenBasedRememberMeServices {
	private final PersistentTokenRepository tokenRepository;
	private final RedisTemplate<String, Object> redisTemplate;
	
	public MyRememberMeService(RedisTemplate<String, Object> redisTemplate, String key, UserDetailsService userDetailsService, PersistentTokenRepository tokenRepository) {
		super(key, userDetailsService, tokenRepository);
		this.tokenRepository = tokenRepository;
		this.redisTemplate = redisTemplate;
		setCookieName("mos-remember-me");
		setParameter("rememberMe");
		setTokenValiditySeconds(3600 * 24 * 30);
	}
	
	protected UserDetails processAutoLoginCookie(String[] cookieTokens, HttpServletRequest request, HttpServletResponse response) {
		
		if (cookieTokens.length != 2) {
			throw new InvalidCookieException("Cookie token did not contain " + 2 + " tokens, but contained '" + Arrays.asList(cookieTokens) + "'");
		}
		
		final String presentedSeries = cookieTokens[0];
		final String presentedToken = cookieTokens[1];
		
		PersistentRememberMeToken cachedToken = getCachedToken(presentedSeries);
		if (isTokenValid(cachedToken, presentedToken)) {
			return getUserDetailsService().loadUserByUsername(cachedToken.getUsername());
		}
		
		PersistentRememberMeToken token = tokenRepository.getTokenForSeries(presentedSeries);
		if (!isTokenValid(token, presentedToken)) {
			if (token != null) {
				tokenRepository.removeUserTokens(token.getUsername());
			}
			throw new RememberMeAuthenticationException("Token is not valid: " + presentedSeries);
		}
		
		PersistentRememberMeToken newToken = new PersistentRememberMeToken(token.getUsername(), token.getSeries(), generateTokenData(), new Date());
		try {
			setCachedToken(token);
			tokenRepository.updateToken(newToken.getSeries(), newToken.getTokenValue(), newToken.getDate());
			setCookie(new String[]{newToken.getSeries(), newToken.getTokenValue()}, getTokenValiditySeconds(), request, response);
		} catch (Exception e) {
			logger.error("Failed to update token: ", e);
			throw new RememberMeAuthenticationException("Autologin failed due to data access problem");
		}
		return getUserDetailsService().loadUserByUsername(token.getUsername());
	}
	
	private boolean isTokenValid(PersistentRememberMeToken token, String presentedToken) {
		if (token == null) {
			return false;
		}
		
		if (!presentedToken.equals(token.getTokenValue())) {
			return false;
		}
		return token.getDate().getTime() + getTokenValiditySeconds() * 1000L >= System.currentTimeMillis();
	}
	
	private PersistentRememberMeToken getCachedToken(String series) {
		String key = "rememberMe:token:" + series;
		Object value = redisTemplate.opsForValue().get(key);
		if (value != null) {
			return JSONObject.parseObject(value.toString(), PersistentRememberMeToken.class);
		}
		return null;
	}
	
	private void setCachedToken(PersistentRememberMeToken token) {
		String series = token.getSeries();
		redisTemplate.opsForValue().set("rememberMe:token:" + series, JSONObject.toJSONString(token), 30, TimeUnit.SECONDS);
	}
	
}
