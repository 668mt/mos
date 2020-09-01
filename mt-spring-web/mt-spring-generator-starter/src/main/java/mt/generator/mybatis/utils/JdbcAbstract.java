package mt.generator.mybatis.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;


/**
 * 
* @ClassName: JdbcAbstract
* @Description: 
* @author Martin
* @date 2017-10-23 下午5:36:23
*
 */
public abstract class JdbcAbstract {
	public final Log log = LogFactory.getLog(JdbcAbstract.class);
	private String driver;
	private String jdbcUrl;
	private String username;
	private String password;
	
	/**
	 * 查询
	 * @param sql select语句
	 * @param connection
	 * @return 更新条数
	 */
	abstract List<Map<String, Object>> select(String sql, Connection connection);
	
	/**
	 * 更新
	 * @param sql insert、update、delete语句
	 * @param connection
	 * @return 更新条数
	 */
	abstract int update(String sql, Connection connection);
	
	/**
	 * 查询
	 * @param sql select语句
	 * @return 更新条数
	 */
	public List<Map<String, Object>> select(String sql, boolean isLog){
		if(isLog){
			log.info("查询语句: "+sql);
		}
		if(StringUtils.isBlank(sql)){
			throw new RuntimeException("sql语句不能为空");
		}
		List<Map<String, Object>> list = select(sql,getConnection());
		if(isLog){
			for (Map<String, Object> map : list) {
				log.info(">>>> row:"+map);
			}
			log.info(">>>> total:"+list.size());
		}
		return list;
	}
	public List<Map<String, Object>> select(String sql){
		return select(sql,true);
	}
	
	/**
	 * 更新
	 * @param sql update语句
	 * @return 更新条数
	 */
	public int update(String sql){
		log.info("更新语句: "+sql);
		if(StringUtils.isBlank(sql)){
			throw new RuntimeException("sql语句不能为空");
		}
		int update = update(sql, getConnection());
		log.info(">>>> update:"+update);
		return update;
	}
	/**
	 * 删除
	 * @param sql delete语句
	 * @return 更新条数
	 */
	public int delete(String sql){
		log.info("删除语句: "+sql);
		if(StringUtils.isBlank(sql)){
			throw new RuntimeException("sql语句不能为空");
		}
		int update = update(sql, getConnection());
		log.info(">>>> update:"+update);
		return update;
	}
	/**
	 * 新增
	 * @param sql insert语句
	 * @return 更新条数
	 */
	public int insert(String sql){
		log.info("插入语句: "+sql);
		if(StringUtils.isBlank(sql)){
			throw new RuntimeException("sql语句不能为空");
		}
		int update = update(sql, getConnection());
		log.info(">>>> update:"+update);
		return update;
	}
	
	/**
	 * 执行
	 * @param sql insert、update、delete语句
	 * @return 更新条数
	 */
	public int execute(String sql){
		log.info("执行语句: "+sql);
		if(StringUtils.isBlank(sql)){
			throw new RuntimeException("sql语句不能为空");
		}
		int update = update(sql, getConnection());
		log.info(">>>> update:"+update);
		return update;
	}
	
	/**
	 * 关闭
	 * @param conn
	 * @param ps
	 * @param rs
	 */
	void close(Connection conn, PreparedStatement ps, ResultSet rs){
		try {
			if(rs != null){
				rs.close();
			}
			if(ps != null){
				ps.close();
			}
			if(conn != null){
				conn.close();
			}
		} catch (Exception e) {
			log.error(e);
		}
	}
	
	/**
	 * 获取连接
	 * @return
	 */
	private Connection getConnection() {
	    Connection conn = null;
	    try {
	        Class.forName(driver); //classLoader,加载对应驱动
	        conn = (Connection) DriverManager.getConnection(jdbcUrl, username, password);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return conn;
	}
	public String getDriver() {
		return driver;
	}
	public void setDriver(String driver) {
		this.driver = driver;
	}
	public String getJdbcUrl() {
		return jdbcUrl;
	}
	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public JdbcAbstract(String driver, String jdbcUrl, String username,
						String password) {
		super();
		this.driver = driver;
		this.jdbcUrl = jdbcUrl;
		this.username = username;
		this.password = password;
	}
	
}
