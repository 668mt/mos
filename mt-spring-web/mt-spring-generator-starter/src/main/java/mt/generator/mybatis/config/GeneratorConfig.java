package mt.generator.mybatis.config;

import mt.generator.mybatis.Generator;
import org.springframework.context.annotation.Bean;

/**
 * @Author Martin
 * @Date 2018/11/3
 */
public class GeneratorConfig {
	@Bean
	public Generator generator(){
		return new Generator();
	}
}
