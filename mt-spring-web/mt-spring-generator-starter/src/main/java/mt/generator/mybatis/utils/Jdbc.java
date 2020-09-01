package mt.generator.mybatis.utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 
* @ClassName: Jdbc
* @Description: 
* @author Martin
* @date 2017-10-23 下午5:36:28
*
 */
public class Jdbc extends JdbcAbstract {

	public Jdbc(String driverClass, String jdbcUrl, String user,
				String password) {
		super(driverClass, jdbcUrl, user, password);
	}

	@Override
	List<Map<String, Object>> select(String sql, Connection conn) {
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		if(conn == null){
			throw new RuntimeException("数据库连接失败");
		}
		try {
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
			ResultSetMetaData metaData = rs.getMetaData();
			int col = metaData.getColumnCount();
			while(rs.next()){
				Map<String, Object> map = new HashMap<String, Object>();
				for(int i=1;i<=col;i++){
					map.put(metaData.getColumnLabel(i), rs.getObject(i));
				}
				list.add(map);
			}
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		} finally {
			close(conn, ps, rs);
		}
		return list;
	}

	@Override
	int update(String sql, Connection conn) {
		int count = 0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		if(conn == null){
			throw new RuntimeException("数据库连接失败");
		}
		try {
			ps = conn.prepareStatement(sql);
			count = ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(conn, ps, rs);
		}
		return count;
	}

	/**
	 * 查询只有一个结果
	 * @param sql
	 * @return
	 */
	public Map<String, Object> selectOne(String sql) {
		List<Map<String, Object>> list = select(sql,false);
		if(list.size() > 1){
			throw new RuntimeException("查询数量超过1");
		}
		if(list.size() == 1){
			return list.get(0);
		}
		return null;
	}

}
