package mt.generator.mybatis.utils;

import mt.utils.RegexUtils;
import mt.common.annotation.ForeignKey;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import javax.persistence.Column;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * 实体工具类
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Company: </p>
 *
 * @author Martin
 * @date 2017-10-22 下午3:37:37
 */
public class SqlServerEntityHelper extends EntityHelper {
	public final Log log = LogFactory.getLog(SqlServerEntityHelper.class);
	
	public SqlServerEntityHelper(String jdbcUrl, String driverClass,
								 String user, String password) {
		super(jdbcUrl, driverClass, user, password);
	}
	
	@Override
	public String getDatabaseName() {
		return RegexUtils.findFirst(getJdbcUrl(), "database(name){0,1}=(\\w+)", 2);
	}
	
	@Override
	public synchronized boolean isExistDatabase(String databaseName) {
		String jdbcUrl = getJdbcUrl();
		String masterDatabaseUrl = jdbcUrl.replaceAll("database(name){0,1}=(\\w+)", "databasename=master");
		Jdbc jdbc = getJdbc(getDriverClass(), masterDatabaseUrl, getUser(), getPassword());
		Map<String, Object> select = jdbc.selectOne("SELECT (CASE WHEN EXISTS (select 1 From master.dbo.sysdatabases where name = '" + databaseName + "') THEN 1 ELSE 0 END) as result");
		return select.get("result").toString().equals("1");
	}
	
	@Override
	public synchronized boolean isExistTable(String tableName) {
		Jdbc jdbc = getJdbc(getDriverClass(), getJdbcUrl(), getUser(), getPassword());
		Map<String, Object> select = jdbc.selectOne("SELECT (CASE WHEN EXISTS (select 1 from dbo.sysobjects where xtype='U' and Name = '" + tableName + "') THEN 1 ELSE 0 END) as result");
		return select.get("result").toString().equals("1");
	}
	
	@Override
	public synchronized boolean checkDatabase() {
		String databaseName = getDatabaseName();
		String masterDatabaseUrl = getJdbcUrl().replaceAll("database(name){0,1}=(\\w+)", "databasename=master");
		if (!isExistDatabase(databaseName)) {
			log.info("初始化自动创建数据库...");
			getJdbc(masterDatabaseUrl).execute("create database " + databaseName);
			log.info("创建数据库：" + databaseName);
			return true;
		}
		return false;
	}
	
	/**
	 * alter table 从表    add constraint 约束名 foreign key(关联字段) references 主表(关联字段) on update casecade
	 */
	@Override
	@SuppressWarnings("deprecation")
	public String getForeightKeySql(Field field, String tableName) {
		Assert.notNull(field);
		ForeignKey annotation = field.getAnnotation(ForeignKey.class);
		if (annotation == null) {
			return "";
		}
		String table = annotation.table();
		if (StringUtils.isBlank(table)) {
			table = getTableName(annotation.tableEntity());
		}
		String referencedColumnName = annotation.referencedColumnName();
		ForeignKey.CascadeType casecadeType = annotation.casecadeType();
		String casecade = "";
		switch (casecadeType) {
			case ALL:
				casecade = " ON DELETE CASCADE ON UPDATE CASCADE ";
				break;
			case DELETE:
				casecade = " ON DELETE CASCADE ";
				break;
			case UPDATE:
				casecade = " ON UPDATE CASCADE ";
				break;
			case DETACH:
				casecade = " ON UPDATE SET NULL ON DELETE SET NULL ";
				break;
			case DEFAULT:
				casecade = "";
				break;
		}
		String columnName = getColumnName(field);
		return "alter table " + tableName + " add constraint FK_" + tableName + "_" + columnName + "_" + table + "_" + referencedColumnName + " foreign key (" + columnName + ") references " + table + " (" + referencedColumnName + ") " + casecade;
	}
	
	@Override
	public String getColumnDefinition(Field field) {
		String columnDefinition = null;
		Class<?> type = field.getType();
		Column column = field.getAnnotation(Column.class);
		if (column != null && StringUtils.isNotBlank(column.columnDefinition())) {
			String c = column.columnDefinition();
			if (c.toLowerCase().equals("text")) {
				return "varchar(max)";
			} else if (c.toLowerCase().equals("blob")) {
				return "image";
			}
			return c;
		}
		if (type.isAssignableFrom(Integer.class)) {
			columnDefinition = "int";
		} else if (type.isAssignableFrom(Boolean.class)) {
			columnDefinition = "int";
		} else if (type.isAssignableFrom(Date.class)) {
			columnDefinition = "datetime";
		} else if (type.isAssignableFrom(Long.class)) {
			columnDefinition = "bigint";
		} else if (type.isAssignableFrom(BigDecimal.class)) {
			int precision = 21;
			int scale = 6;
			if (column != null) {
				if (column.precision() > 0) {
					precision = column.precision();
				}
				if (column.scale() > 0) {
					scale = column.scale();
				}
			}
			columnDefinition = "numeric(" + precision + "," + scale + ")";
		} else if (type.isAssignableFrom(String.class)) {
			if (column != null) {
				columnDefinition = "varchar(" + column.length() + ")";
			} else {
				columnDefinition = "varchar(100)";
			}
		} else if (type.isAssignableFrom(Enum.class)) {
			columnDefinition = "varchar(100)";
		} else {
			columnDefinition = "varchar(500)";
		}
		return columnDefinition;
	}
	
	
}
