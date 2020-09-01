package mt.generator.mybatis.utils;

import mt.common.annotation.ForeignKey;
import mt.common.annotation.GenerateOrder;
import mt.utils.ReflectUtils;
import mt.utils.RegexUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
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
public class MySQLEntityHelper extends EntityHelper {
	public final Log log = LogFactory.getLog(EntityHelper.class);
	
	public MySQLEntityHelper(String jdbcUrl, String driverClass, String user,
							 String password) {
		super(jdbcUrl, driverClass, user, password);
	}
	
	@Override
	public String getDatabaseName() {
		return RegexUtils.findFirst(getJdbcUrl(), "jdbc:mysql://(.+?)/(\\w+)(\\?.+)", 2);
	}
	
	@Override
	public synchronized boolean checkDatabase() {
		String databaseName = getDatabaseName();
		String mysqlDatabaseName = getJdbcUrl().replaceAll("jdbc:mysql://(.+?)/(\\w+)(\\?.+)", "jdbc:mysql://$1/mysql$3");
		if (!isExistDatabase(databaseName)) {
			log.info("初始化自动创建数据库...");
			getJdbc(mysqlDatabaseName).execute("create database " + databaseName);
			log.info("创建数据库：" + databaseName);
			return true;
		}
		return false;
	}
	
	public synchronized boolean isExistDatabase(String databaseName) {
		String mysqlDatabaseName = getJdbcUrl().replaceAll("jdbc:mysql://(.+?)/(\\w+)(\\?.+)", "jdbc:mysql://$1/mysql$3");
		Jdbc jdbc = getJdbc(getDriverClass(), mysqlDatabaseName, getUser(), getPassword());
		Map<String, Object> select = jdbc.selectOne("SELECT (CASE WHEN EXISTS (SELECT 1 FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '" + databaseName + "') THEN 1 ELSE 0 END) as result;");
		return select.get("result").toString().equals("1");
	}
	
	@Override
	public synchronized boolean isExistTable(String tableName) {
		Jdbc jdbc = getJdbc(getDriverClass(), getJdbcUrl(), getUser(), getPassword());
		Map<String, Object> select = jdbc.selectOne("SELECT (CASE WHEN EXISTS (SELECT table_name FROM information_schema.TABLES WHERE table_name = '" + tableName + "' AND table_schema = '" + getDatabaseName() + "') THEN 1 ELSE 0 END) as result;");
		return select.get("result").toString().equals("1");
	}
	
	@Override
	public String getPriamryKeySql(List<Field> fields) {
		StringBuilder sql = new StringBuilder();
		for (Field field : fields) {
			if (field.getAnnotation(Id.class) != null) {
				sql.append("`").append(getColumnName(field)).append("`,");
			}
		}
		Assert.state(!sql.toString().equals(""), "主键不能为空:" + currentTableName);
		sql = new StringBuilder("primary key(" + sql.substring(0, sql.lastIndexOf(",")) + ")");
		return sql.toString();
	}
	
	@Override
	public String getCreateTableSql(Class<?> entityClass) {
		String sql = "";
		String tableName = getTableName(entityClass);
		sql += "create table `" + tableName + "` (";
		List<Field> findAllFields = ReflectUtils.findAllFieldsIgnore(entityClass, Transient.class);
		findAllFields.sort((o1, o2) -> {
			GenerateOrder annotation1 = o1.getAnnotation(GenerateOrder.class);
			int order1 = annotation1 == null ? 0 : annotation1.value();
			GenerateOrder annotation2 = o2.getAnnotation(GenerateOrder.class);
			int order2 = annotation2 == null ? 0 : annotation2.value();
			return order2 - order1;
		});
		StringBuilder contentSql = new StringBuilder();
		for (Field field : findAllFields) {
			if (!Modifier.isFinal(field.getModifiers())) {
				contentSql.append("\t").append(getJdbcTypeSql(field)).append(",\r\n");
			}
		}
		//添加主外键信息
		contentSql.append("\t").append(getPriamryKeySql(findAllFields)).append("\r\n");
		
		
		//获取主外键信息
		for (Field field : findAllFields) {
			if (!Modifier.isFinal(field.getModifiers())) {
				String foreightKeySql = getForeightKeySql(field, tableName);
				if (StringUtils.isNoneBlank(foreightKeySql)) {
					foreightKeys.add(foreightKeySql);
				}
			}
		}
		return sql + "\r\n" + contentSql + "\r\n)";
	}
	
	@SuppressWarnings("deprecation")
	@Override
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
		return "ALTER TABLE `" + tableName + "` ADD CONSTRAINT `PK_" + tableName + "_" + columnName + "_" + table + "_" + referencedColumnName + "` FOREIGN KEY(`" + columnName + "`) REFERENCES `" + table + "`(`" + referencedColumnName + "`) " + casecade;
	}
	
	@Override
	public String getJdbcTypeSql(Field field) {
		Column column = field.getAnnotation(Column.class);
		//是否为空
		boolean nullable = true;
		String name = getColumnName(field);
		//是否唯一
		boolean unique = false;
		//类型定义
		String columnDefinition = getColumnDefinition(field);
		
		if (column != null) {
			nullable = column.nullable();
			unique = column.unique();
		}
		String nullSql = nullable ? "" : "not null";
		String uniqueSql = unique ? "unique" : "";
		return " `" + name + "` " + " " + columnDefinition + " " + nullSql + " " + uniqueSql;
	}
	
	@Override
	public String getIdentitySql(Field field) {
		KeySql keySql = field.getAnnotation(KeySql.class);
		if (keySql != null && keySql.useGeneratedKeys()) {
			return "AUTO_INCREMENT";
		}
		return super.getIdentitySql(field);
	}
}
