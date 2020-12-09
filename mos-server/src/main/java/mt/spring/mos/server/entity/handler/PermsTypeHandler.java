package mt.spring.mos.server.entity.handler;

import mt.spring.mos.server.entity.BucketPerm;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2020/12/9
 */
public class PermsTypeHandler implements TypeHandler<List<BucketPerm>> {
	@Override
	public void setParameter(PreparedStatement ps, int i, List<BucketPerm> parameter, JdbcType jdbcType) throws SQLException {
		String value = null;
		if (parameter != null) {
			value = parameter.stream().map(Enum::name).collect(Collectors.joining(","));
		}
		ps.setString(i, value);
	}
	
	@Override
	public List<BucketPerm> getResult(ResultSet rs, String columnName) throws SQLException {
		String value = rs.getString(columnName);
		if (StringUtils.isNotBlank(value)) {
			return Arrays.stream(value.split(",")).map(BucketPerm::valueOf).collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
	
	@Override
	public List<BucketPerm> getResult(ResultSet rs, int columnIndex) throws SQLException {
		String value = rs.getString(columnIndex);
		if (StringUtils.isNotBlank(value)) {
			return Arrays.stream(value.split(",")).map(BucketPerm::valueOf).collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
	
	@Override
	public List<BucketPerm> getResult(CallableStatement cs, int columnIndex) throws SQLException {
		String value = cs.getString(columnIndex);
		if (StringUtils.isNotBlank(value)) {
			return Arrays.stream(value.split(",")).map(BucketPerm::valueOf).collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
}
