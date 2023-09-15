package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.common.annotation.ForeignKey;
import mt.generator.mybatis.annotation.Index;
import mt.generator.mybatis.annotation.Indexs;
import mt.spring.mos.server.entity.BaseEntity;
import mt.spring.mos.server.entity.handler.Map2JsonTypeHandler;
import org.apache.ibatis.type.JdbcType;
import tk.mybatis.mapper.annotation.ColumnType;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2020/10/23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "mos_client_work_log")
@Indexs({
	@Index(name = "idx_lock_key", columns = {"lock_key"}),
	@Index(name = "idx_exe_status", columns = {"exe_status"}),
	@Index(name = "idx_client_id", columns = {"client_id"}),
})
public class ClientWorkLog extends BaseEntity {
	@Id
	@KeySql(useGeneratedKeys = true)
	private Long id;
	
	@Column(nullable = false)
	private Long clientId;
	
	@ColumnType(jdbcType = JdbcType.VARCHAR)
	private Action action;
	private String lockKey;
	
	@Column(columnDefinition = "text")
	@ColumnType(typeHandler = Map2JsonTypeHandler.class)
	private Map<String, Object> params;
	
	@ColumnType(jdbcType = JdbcType.VARCHAR)
	private ExeStatus exeStatus;
	
	@Column(columnDefinition = "text")
	private String message;
	
	public enum ExeStatus {
		NOT_START, SUCCESS, FAIL, IGNORE
	}
	
	public enum Action {
		/**
		 * 操作类型
		 */
		ADD_FILE,
		DELETE_FILE,
		DELETE_DIR,
		MOVE_FILE
	}
	
}
