package mt.generator.mybatis.typeHandlers;

import com.fasterxml.jackson.core.type.TypeReference;
import mt.utils.JsonUtils;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 
* @ClassName: MapTypeHandler
* @Description: 
* @author Martin
* @date 2017-9-30 上午10:25:36
*
 */
@MappedTypes(value = Map.class)
@MappedJdbcTypes(value = JdbcType.VARCHAR)
public class MapTypeHandler implements TypeHandler<Map<String, String>>{

	@Override
	public void setParameter(PreparedStatement ps, int i, Map<String, String> parameter, JdbcType jdbcType) throws SQLException {
		if(parameter != null){
			ps.setString(i, JsonUtils.toJson(parameter));
		}
	}

	@Override
	public Map<String, String> getResult(ResultSet rs, String columnName)
			throws SQLException {
		String json = rs.getString(columnName);
		try {
			if(json != null && json.contains("{") && json.contains("}")){
				return JsonUtils.toObject(json, new TypeReference<Map<String, String>>(){});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new HashMap<String, String>();
	}

	@Override
	public Map<String, String> getResult(ResultSet rs, int columnIndex)
			throws SQLException {
		String json = rs.getString(columnIndex);
		if(json != null){
			return JsonUtils.toObject(json, new TypeReference<Map<String, String>>(){});
		}
		return new HashMap<String, String>();
	}

	@Override
	public Map<String, String> getResult(CallableStatement cs, int columnIndex)
			throws SQLException {
		String json = cs.getString(columnIndex);
		if(json != null){
			return JsonUtils.toObject(json, new TypeReference<Map<String, String>>(){});
		}
		return new HashMap<String, String>();
	}

}
