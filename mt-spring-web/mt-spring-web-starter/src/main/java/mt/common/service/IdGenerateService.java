package mt.common.service;

import mt.common.annotation.BaseIdGenerator;
import mt.common.annotation.GenerateClass;
import mt.common.annotation.IdGenerator;
import mt.common.config.CommonProperties;
import mt.common.entity.IdGenerate;
import org.apache.commons.beanutils.ConvertUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;

public class IdGenerateService {
	
	public enum Generator {
		/**
		 * 默认,自增长
		 */
		IDENTITY,
		/**
		 * 自增长，前面自动补0
		 */
		FILLZERO,
		/**
		 * 自定义，通过@GenerateClass指定方法
		 */
		DIY
	}
	
	@Autowired
	private DataLockService dataLockService;
	@Autowired
	private CommonProperties commonProperties;
	
	private BaseIdGenerator diyGenerator;
	
	public int insert(IdGenerate idGenerate, JdbcTemplate jdbcTemplate) throws Exception {
		String sql = "INSERT INTO " + commonProperties.getIdGenerateTableName() + " (tableName,nextValue) VALUES (?,?)";
		return jdbcTemplate.update(sql, idGenerate.getTableName(), idGenerate.getNextValue());
	}
	
	public int updateByPrimaryKey(IdGenerate idGenerate, JdbcTemplate jdbcTemplate) throws Exception {
		String sql = "UPDATE " + commonProperties.getIdGenerateTableName() + " SET nextValue = ? WHERE tableName = ?";
		return jdbcTemplate.update(sql, idGenerate.getNextValue(), idGenerate.getTableName());
	}
	
	public IdGenerate selectByPrimaryKey(String tableName, JdbcTemplate jdbcTemplate) throws Exception {
		String sql = "SELECT tableName,nextValue FROM " + commonProperties.getIdGenerateTableName() + " WHERE tableName = ?";
		return jdbcTemplate.query(sql, new Object[]{tableName}, new ResultSetExtractor<IdGenerate>() {
			@Nullable
			@Override
			public IdGenerate extractData(ResultSet rs) throws SQLException, DataAccessException {
				if (rs.next()) {
					return new IdGenerate(rs.getString(1), rs.getLong(2));
				}
				return null;
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 120)
	public <T> T generate(@NotNull String tableName, @NotNull IdGenerator idGenerator, @Nullable GenerateClass generateClass, @NotNull Class<T> type, @NotNull JdbcTemplate jdbcTemplate) throws Exception {
		dataLockService.lock("idGenerate", jdbcTemplate);
//		IdGenerator.Generator generator = idGenerator.generator();
		Generator generator = idGenerator.generator();
		T id = null;
		switch (generator) {
			case IDENTITY:
				IdGenerate idGenerate1 = selectByPrimaryKey(tableName, jdbcTemplate);
				Long idLong = 1L;
				if (idGenerate1 == null) {
					//新增一个表
					insert(new IdGenerate(tableName, 2L), jdbcTemplate);
				} else {
					Long nextValue = idGenerate1.getNextValue();
					idLong = nextValue;
					idGenerate1.setNextValue(nextValue + 1);
					updateByPrimaryKey(idGenerate1, jdbcTemplate);
				}
				id = (T) ConvertUtils.convert(idLong, type);
				break;
			case FILLZERO:
				IdGenerate idGenerate2 = selectByPrimaryKey(tableName, jdbcTemplate);
				String idStr = "";
				if (idGenerate2 == null) {
					idStr += 1;
					//新增一个表
					insert(new IdGenerate(tableName, 2L), jdbcTemplate);
				} else {
					Long nextValue = idGenerate2.getNextValue();
					idStr += nextValue;
					idGenerate2.setNextValue(nextValue + 1);
					updateByPrimaryKey(idGenerate2, jdbcTemplate);
				}
				int len = idStr.length();
				int maxLen = len > idGenerator.maxLength() ? len : idGenerator.maxLength();
				for (int i = 0; i < maxLen - len; i++) {
					idStr = "0" + idStr;
				}
				id = (T) ConvertUtils.convert(idStr, type);
				break;
			case DIY:
				Assert.notNull(generateClass, "@generateClass不能为空");
				Class<? extends BaseIdGenerator> class1 = generateClass.value();
				if (diyGenerator == null) {
					diyGenerator = class1.newInstance();
				}
				Method method = class1.getMethod("generate", String.class, IdGenerator.class);
				Object diyId = method.invoke(diyGenerator, tableName, idGenerator);
				id = (T) ConvertUtils.convert(diyId, type);
				break;
		}
		return id;
	}
}
