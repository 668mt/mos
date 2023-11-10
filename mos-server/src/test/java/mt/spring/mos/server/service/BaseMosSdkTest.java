package mt.spring.mos.server.service;

import mt.spring.mos.sdk.MosSdk;
import org.junit.Before;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;

/**
 * @Author Martin
 * @Date 2023/9/8
 */
public class BaseMosSdkTest {
	protected MosSdk mosSdk;
	
	@Before
	public void before() {
		LoggingSystem.get(getClass().getClassLoader()).setLogLevel("root", LogLevel.INFO);
		mosSdk = new MosSdk("http://localhost:9700", 5, "default", "b-T3wXaUu5umA3vumqEIVA==");
	}
}
