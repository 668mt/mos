package mt.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author Martin
 * @Date 2018/11/1
 */
@ConfigurationProperties(prefix = "logging")
@Component
@Data
public class LoggingProperties {
	private String dir;
	private String appname;
}
