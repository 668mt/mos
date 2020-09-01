package mt.common.config;

import com.github.pagehelper.PageHelper;
import mt.common.currentUser.UserContext;
import mt.common.mybatis.CreatedByInterceptor;
import mt.common.mybatis.LastModifiedByInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * @Author Martin
 * @Date 2020/1/11
 */
@Configuration
public class MybatisConfiguration {
	@Bean
	public PageHelper pageHelper() {
		PageHelper pageHelper = new PageHelper();
		Properties properties = new Properties();
		properties.setProperty("offsetAsPageNum", "true");
		properties.setProperty("rowBoundsWithCount", "true");
		properties.setProperty("pageSizeZero", "true");
		properties.setProperty("reasonable", "true");
		properties.setProperty("params", "pageNum=pageHelperStart;pageSize=pageHelperRows;");
		properties.setProperty("supportMethodsArguments", "false");
		properties.setProperty("returnPageInfo", "none");
		pageHelper.setProperties(properties);
		return pageHelper;
	}
	
	@Bean
	@ConditionalOnBean(UserContext.class)
	public LastModifiedByInterceptor lastModifiedByInterceptor(UserContext userContext) {
		return new LastModifiedByInterceptor(userContext);
	}
	
	@Bean
	@ConditionalOnBean(UserContext.class)
	public CreatedByInterceptor createdByInterceptor(UserContext userContext) {
		return new CreatedByInterceptor(userContext);
	}
}
