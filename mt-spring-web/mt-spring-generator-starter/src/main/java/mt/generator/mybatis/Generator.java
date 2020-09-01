package mt.generator.mybatis;

import lombok.extern.slf4j.Slf4j;
import mt.common.config.CommonProperties;
import mt.common.entity.DataLock;
import mt.common.mybatis.event.AfterInitEvent;
import mt.common.mybatis.event.BeforeInitEvent;
import mt.common.service.DataLockService;
import mt.generator.mybatis.utils.MySQLEntityHelper;
import mt.generator.mybatis.utils.SqlServerEntityHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import tk.mybatis.mapper.autoconfigure.MybatisProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库和表格自动生成器
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Company: </p>
 *
 * @author Martin
 * @date 2017-10-23 下午10:10:04
 * implements InitializingBean, ApplicationListener<ApplicationEvent>
 */
@Slf4j
public class Generator {
	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;
	@Autowired
	private CommonProperties commonProperties;
	@Autowired
	private MybatisProperties mybatisProperties;
	@Autowired
	private DataSourceProperties dataSourceProperties;
	@Autowired
	private DataLockService dataLockService;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@EventListener
	public void listener(ContextRefreshedEvent contextRefreshedEvent) throws Exception {
		if (contextRefreshedEvent.getApplicationContext().getParent() == null) {
			return;
		}
		if (!commonProperties.getGeneratorEnable()) {
			return;
		}
		String[] entityPackages;
		String jdbcUrl;
		String driverClass;
		String user;
		String password;
		jdbcUrl = dataSourceProperties.getUrl();
		driverClass = dataSourceProperties.getDriverClassName();
		user = dataSourceProperties.getUsername();
		password = dataSourceProperties.getPassword();
		entityPackages = commonProperties.getGenerateEntityPackages();
		if (entityPackages == null || entityPackages.length == 0) {
			entityPackages = new String[]{mybatisProperties.getTypeAliasesPackage()};
		}
		
		applicationEventPublisher.publishEvent(new BeforeInitEvent(this));
		
		//初始化
		log.info("初始化...");
		//初始化完成事件
		AfterInitEvent afterInitEvent = new AfterInitEvent(this);
		
		try {
			if ("com.mysql.cj.jdbc.Driver".equals(driverClass.trim()) || StringUtils.equals("com.mysql.jdbc.Driver", driverClass.trim())) {
				//mysql数据库
				new MySQLEntityHelper(jdbcUrl, driverClass, user, password).init(entityPackages, afterInitEvent);
			} else if (StringUtils.equals("com.microsoft.sqlserver.jdbc.SQLServerDriver", driverClass.trim())) {
				//sqlserver数据库
				new SqlServerEntityHelper(jdbcUrl, driverClass, user, password).init(entityPackages, afterInitEvent);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		
		List<String> locks = new ArrayList<>();
		locks.add("idGenerate");
		locks.add("locks");
		locks.add("sn");
		for (String id : locks) {
			if (!dataLockService.existsId(id, jdbcTemplate)) {
				DataLock dataLock = new DataLock();
				dataLock.setId(id);
				dataLock.setUseKey(id);
				dataLockService.insert(dataLock, jdbcTemplate);
			}
		}
		
		//注册初始化完成事件
		applicationEventPublisher.publishEvent(afterInitEvent);
		
	}
	
}
