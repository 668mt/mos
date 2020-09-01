package mt.common.config;

import mt.utils.BasePackageUtils;
import mt.utils.MyUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2019/8/27
 */
public class CommonEnvironmentPostProcessor implements EnvironmentPostProcessor{
	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		MutablePropertySources propertySources = environment.getPropertySources();
		Map<String,Object> params = new HashMap<>();
		
		String basePackage = MyUtils.nullAsDefault(environment.getProperty("project.base-package",String.class),environment.getProperty("project.basePackage",String.class), BasePackageUtils.getBasePackage(application.getMainApplicationClass()));
		params.put("project.base-package", basePackage);
		params.put("project.basePackage", basePackage);
		propertySources.addFirst(new CommonPropertySource("commonPropertyResource",params));
	}
}
