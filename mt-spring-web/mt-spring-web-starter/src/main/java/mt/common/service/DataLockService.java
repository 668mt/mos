package mt.common.service;

import mt.common.config.CommonProperties;
import mt.common.entity.DataLock;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;


/**
 * @author Martin
 * @ClassName: DataLockServiceImpl
 * @Description:
 * @date 2017-12-5 下午4:21:11
 */
@Service
public class DataLockService {
	
	@Autowired
	private CommonProperties commonProperties;
	
	public int deleteByPrimaryKey(String id, JdbcTemplate jdbcTemplate) {
		return jdbcTemplate.update("delete from " + commonProperties.getDataLockTableName() + " where id = ?", id);
	}
	
	public int updateByPrimaryKey(DataLock dataLock, JdbcTemplate jdbcTemplate) {
		String sql = "UPDATE " + commonProperties.getDataLockTableName() + " SET useKey = ? WHERE id = ?";
		return jdbcTemplate.update(sql, dataLock.getUseKey(), dataLock.getId());
	}
	
	public DataLock selectByPrimaryKey(String id, JdbcTemplate jdbcTemplate) {
		String sql = "SELECT id,useKey FROM " + commonProperties.getDataLockTableName() + " WHERE id = ?";
		return jdbcTemplate.query(sql, new Object[]{id}, new ResultSetExtractor<DataLock>() {
			@Nullable
			@Override
			public DataLock extractData(ResultSet resultSet) throws SQLException, DataAccessException {
				if (resultSet.next()) {
					String id = resultSet.getString(1);
					String useKey = resultSet.getString(2);
					DataLock dataLock = new DataLock();
					dataLock.setId(id);
					dataLock.setUseKey(useKey);
					return dataLock;
				}
				return null;
			}
		});
	}
	
	public int insert(@NotNull DataLock dataLock, JdbcTemplate jdbcTemplate) {
		String sql = "INSERT INTO " + commonProperties.getDataLockTableName() + " (id,useKey) values (?,?)";
		return jdbcTemplate.update(sql, dataLock.getId(), dataLock.getUseKey());
	}
	
	public boolean existsId(String id, JdbcTemplate jdbcTemplate) {
		DataLock dataLock = selectByPrimaryKey(id, jdbcTemplate);
		return dataLock != null;
	}
	
	public int lock(@NotNull String id, @NotNull JdbcTemplate jdbcTemplate) {
		jdbcTemplate.queryForList("select * from " + commonProperties.getDataLockTableName() + " where id = ? for update", id);
		DataLock dataLock = new DataLock();
		dataLock.setId(id);
		dataLock.setUseKey(id + "_" + UUID.randomUUID().toString());
		if (!existsId(id, jdbcTemplate)) {
//			insert(dataLock, jdbcTemplate);
			throw new RuntimeException("锁定 " + id + " 时失败，不存在该锁");
		}
		int update = updateByPrimaryKey(dataLock, jdbcTemplate);
		if (update > 0) {
			return update;
		}
		throw new RuntimeException("锁定 " + id + " 时失败");
	}
	
	public interface Function<T> {
		T doInTransaction();
	}

//	public <T> T doWithLock(TransactionTemplate transactionTemplate, JdbcTemplate jdbcTemplate, String type, String lockId, Function<T> function) {
//		transactionTemplate.execute(status -> {
//			lock(type, jdbcTemplate);
//			DataLock test = selectByPrimaryKey(type + ":" + lockId, jdbcTemplate);
//			if (test == null) {
//				test = new DataLock();
//				test.setId(type + ":" + lockId);
//				test.setUseKey("init");
//				insert(test, jdbcTemplate);
//			}
//			return null;
//		});
//		return transactionTemplate.execute(transactionStatus -> {
//			lock(type + ":" + lockId, jdbcTemplate);
//			return function.doInTransaction();
//		});
//	}
	
	public void initLock(String lockId, JdbcTemplate jdbcTemplate) {
		if (!existsId(lockId, jdbcTemplate)) {
			DataLock dataLock = new DataLock();
			dataLock.setId(lockId);
			dataLock.setUseKey("init");
			insert(dataLock, jdbcTemplate);
		}
	}
	
}
