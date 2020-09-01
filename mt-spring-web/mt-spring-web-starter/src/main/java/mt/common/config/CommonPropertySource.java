package mt.common.config;

import org.springframework.core.env.PropertySource;
import org.springframework.lang.Nullable;

import java.util.Map;

/**
 * @Author Martin
 * @Date 2019/8/27
 */
public class CommonPropertySource extends PropertySource<Map<String,Object>>{
	private Map<String,Object> defaultValues;
	public CommonPropertySource(String name, Map<String, Object> source) {
		super(name, source);
		this.defaultValues = source;
	}
	
	@Nullable
	@Override
	public Object getProperty(String name) {
		return defaultValues.get(name);
	}
}
