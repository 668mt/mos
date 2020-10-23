package mt.spring.mos.server.entity.handler;

import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2020/10/23
 */
public class Map2JsonTypeHandler implements TypeHandler<Map<String, Object>> {
	@Override
	public void setParameter(PreparedStatement ps, int i, Map<String, Object> parameter, JdbcType jdbcType) throws SQLException {
		if (parameter != null) {
			ps.setString(i, JSONObject.toJSONString(parameter));
		} else {
			ps.setString(i, JSONObject.toJSONString(new HashMap<>()));
		}
	}
	
	@Override
	public Map<String, Object> getResult(ResultSet rs, String columnName) throws SQLException {
		String json = rs.getString(columnName);
		return json == null ? new HashMap<>() : JSONObject.parseObject(json);
	}
	
	@Override
	public Map<String, Object> getResult(ResultSet rs, int columnIndex) throws SQLException {
		String json = rs.getString(columnIndex);
		return json == null ? new HashMap<>() : JSONObject.parseObject(json);
	}
	
	@Override
	public Map<String, Object> getResult(CallableStatement cs, int columnIndex) throws SQLException {
		String json = cs.getString(columnIndex);
		return json == null ? new HashMap<>() : JSONObject.parseObject(json);
	}
}
