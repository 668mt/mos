package mt.generator.mybatis.utils;

import mt.common.annotation.GenerateOrder;
import mt.common.config.CommonProperties;
import mt.common.entity.DataLock;
import mt.common.entity.IdGenerate;
import mt.common.mybatis.event.AfterInitEvent;
import mt.common.mybatis.utils.MapperColumnUtils;
import mt.common.utils.SpringUtils;
import mt.utils.ClassUtils;
import mt.utils.MyUtils;
import mt.utils.ReflectUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.*;

/**
 * 实体工具类
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Company: </p>
 *
 * @author Martin
 * @date 2017-10-22 下午3:37:37
 */
public abstract class EntityHelper {
	public final Log log = LogFactory.getLog(EntityHelper.class);
	/**
	 * 外键关系
	 */
	public static List<String> foreightKeys = new ArrayList<>();
	protected String currentTableName;
	
	/**
	 * 数据源
	 *
	 * @author Martin
	 * @ClassName: DataSource
	 * @Description:
	 * @date 2017-10-23 下午12:56:26
	 */
	public enum DataSource {
		MySQL, SqlServer, Oracle
	}
	
	/**
	 * 获取数据源
	 *
	 * @param driverClass
	 * @param jdbcUrl
	 * @param user
	 * @param password
	 * @return
	 */
	public Jdbc getJdbc(String driverClass, String jdbcUrl, String user, String password) {
		return new Jdbc(driverClass, jdbcUrl, user, password);
	}
	
	/**
	 * 获取数据库名
	 *
	 * @return
	 */
	public abstract String getDatabaseName();
	
	public Jdbc getJdbc() {
		return new Jdbc(driverClass, jdbcUrl, user, password);
	}
	
	public Jdbc getJdbc(String jdbcUrl) {
		return new Jdbc(driverClass, jdbcUrl, user, password);
	}
	
	/**
	 * 是否存在数据库
	 *
	 * @param databaseName
	 * @return
	 */
	public abstract boolean isExistDatabase(String databaseName);
	
	/**
	 * 是否存在表
	 *
	 * @param tableName
	 * @return
	 */
	public abstract boolean isExistTable(String tableName);
	
	/**
	 * 检查是否有数据库，没有就新建
	 *
	 * @return 是否创建
	 */
	public abstract boolean checkDatabase();
	
	/**
	 * 初始化自动创建表
	 *
	 * @param entityPackages
	 * @param afterInitEvent
	 */
	public void init(String[] entityPackages, AfterInitEvent afterInitEvent) {
		Assert.notNull(afterInitEvent, "afterInitEvent事件不能为空");
		//检查数据库
		afterInitEvent.setCreateDatabase(checkDatabase());
		
		Set<Class<?>> allEntitys = new HashSet<>();
		if (entityPackages != null) {
			for (String entityPackage : entityPackages) {
				allEntitys.addAll(getAllEntitys(entityPackage));
			}
		}
		allEntitys.addAll(getAllEntitys("mt.common.entity"));
		allEntitys.add(IdGenerate.class);
		allEntitys.add(DataLock.class);
		
		List<String> list = new ArrayList<>();
		for (Class<?> entityClass : allEntitys) {
			String tableName = getTableName(entityClass);
			this.currentTableName = tableName;
			if (!isExistTable(tableName)) {
				//建表
				getJdbc().execute(getCreateTableSql(entityClass));
				list.add(tableName);
			}
		}
		List<String> foreightSuccess = new ArrayList<>();
		for (String sql : foreightKeys) {
			//建外键
			getJdbc().execute(sql);
			foreightSuccess.add(sql);
		}
		
		//创建的表
		afterInitEvent.setNewTables(list);
		//创建的主外键关系
		afterInitEvent.setNewForeightKeys(foreightSuccess);
		afterInitEvent.setCreateTable(MyUtils.isNotEmpty(list));
		afterInitEvent.setCreateForeightKey(MyUtils.isNotEmpty(foreightSuccess));
		
		
		log.info("新建表数量：" + list.size());
		for (String table : list) {
			log.info(">>>>>>>>>" + table);
		}
		log.info("新建外键数量：" + foreightSuccess.size());
		for (String sql : foreightSuccess) {
			log.info(">>>>>>>>>" + sql);
		}
	}
	
	/**
	 * 获取表名
	 *
	 * @param entityClass
	 * @return
	 */
	public String getTableName(Class<?> entityClass) {
		if (entityClass.equals(IdGenerate.class))
			return SpringUtils.getBean(CommonProperties.class).getIdGenerateTableName();
		if (entityClass.equals(DataLock.class))
			return SpringUtils.getBean(CommonProperties.class).getDataLockTableName();
		Table table = entityClass.getAnnotation(Table.class);
		String tableName = entityClass.getSimpleName();
		if (table != null) {
			if (StringUtils.isNotBlank(table.name())) {
				tableName = table.name();
			}
		}
		return tableName;
	}
	
	/**
	 * 拼接sql语句
	 *
	 * @param field
	 * @return
	 */
	public String getJdbcTypeSql(Field field) {
		Column column = field.getAnnotation(Column.class);
		boolean nullable = true;
		String name = getColumnName(field);
		boolean unique = false;
		String columnDefinition = getColumnDefinition(field);
		
		if (column != null) {
			nullable = column.nullable();
			unique = column.unique();
		}
		String nullSql = nullable ? "" : "not null";
		String uniqueSql = unique ? "unique" : "";
		return " " + name + " " + " " + columnDefinition + " " + nullSql + " " + uniqueSql;
	}
	
	public String getColumnName(Field field) {
		Column column = field.getAnnotation(Column.class);
		String name = field.getName();
		if (column != null && StringUtils.isNotBlank(column.name())) {
			return column.name();
		}
		return MapperColumnUtils.parseColumn(name);
	}
	
	/**
	 * 获取数据库类型  例：varchar(100)
	 *
	 * @param field
	 * @return
	 */
	public String getColumnDefinition(Field field) {
		String columnDefinition;
		Class<?> type = field.getType();
		Column column = field.getAnnotation(Column.class);
		if (column != null && StringUtils.isNotBlank(column.columnDefinition())) {
			String c = column.columnDefinition();
			if (c.toLowerCase().equals("varchar(max)")) {
				return "text";
			} else if (c.toLowerCase().equals("image")) {
				return "blob";
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
		return columnDefinition + " " + getIdentitySql(field);
	}
	
	public String getIdentitySql(Field field) {
		return "";
	}
	
	/**
	 * 获取主键sql语句
	 *
	 * @param fields
	 * @return
	 */
	public String getPriamryKeySql(List<Field> fields) {
		StringBuilder sql = new StringBuilder();
		for (Field field : fields) {
			if (field.getAnnotation(Id.class) != null) {
				sql.append(getColumnName(field)).append(",");
			}
		}
		sql = new StringBuilder("primary key(" + sql.substring(0, sql.lastIndexOf(",")) + ")");
		return sql.toString();
	}
	
	/**
	 * 拼接如果不存在创建表语句
	 *
	 * @param entityClass
	 * @return
	 */
	public String getCreateTableSql(Class<?> entityClass) {
		StringBuilder sb = new StringBuilder();
		String tableName = getTableName(entityClass);
		sb.append("create table ").append(tableName).append(" (\r\n");
		List<Field> findAllFields = ReflectUtils.findAllFieldsIgnore(entityClass, Transient.class);
		findAllFields.sort((o1, o2) -> {
			GenerateOrder annotation1 = o1.getAnnotation(GenerateOrder.class);
			int order1 = annotation1 == null ? 0 : annotation1.value();
			GenerateOrder annotation2 = o2.getAnnotation(GenerateOrder.class);
			int order2 = annotation2 == null ? 0 : annotation2.value();
			return order2 - order1;
		});
		for (Field field : findAllFields) {
			if (!Modifier.isFinal(field.getModifiers())) {
				sb.append("\t").append(getJdbcTypeSql(field)).append(",\r\n");
			}
		}
		//添加主外键信息
		sb.append("\t").append(getPriamryKeySql(findAllFields)).append("\r\n");
		//获取主外键信息
		for (Field field : findAllFields) {
			if (!Modifier.isFinal(field.getModifiers())) {
				String foreightKeySql = getForeightKeySql(field, tableName);
				if (StringUtils.isNotBlank(foreightKeySql)) {
					foreightKeys.add(foreightKeySql);
				}
			}
		}
		return sb.toString() + ")";
	}
	
	/**
	 * 获取所有带@Table注解的实体
	 *
	 * @return
	 */
	public List<Class<?>> getAllEntitys(String entityPackage) {
		List<Class<?>> classes = ClassUtils.getClasses(entityPackage);
		List<Class<?>> entitys = new ArrayList<>();
		CollectionUtils.select(classes, object -> {
			Class<?> class1 = (Class<?>) object;
			return class1 != null && class1.getAnnotation(Table.class) != null;
		}, entitys);
		return entitys;
	}
	
	private String jdbcUrl;
	private String driverClass;
	private String user;
	private String password;
	
	public EntityHelper(String jdbcUrl, String driverClass, String user,
						String password) {
		super();
		this.jdbcUrl = jdbcUrl;
		this.driverClass = driverClass;
		this.user = user;
		this.password = password;
	}
	
	public String getJdbcUrl() {
		return jdbcUrl;
	}
	
	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}
	
	public String getDriverClass() {
		return driverClass;
	}
	
	public void setDriverClass(String driverClass) {
		this.driverClass = driverClass;
	}
	
	public String getUser() {
		return user;
	}
	
	public void setUser(String user) {
		this.user = user;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	/**
	 * 查询外键
	 *
	 * @return
	 */
	public abstract String getForeightKeySql(Field field, String tableName);
}
