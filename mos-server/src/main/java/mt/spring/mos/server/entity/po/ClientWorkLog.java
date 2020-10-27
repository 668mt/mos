package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
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
public class ClientWorkLog extends BaseEntity {
	@Id
	@KeySql(useGeneratedKeys = true)
	private Long id;
	
	private String clientId;
	
	@ColumnType(jdbcType = JdbcType.VARCHAR)
	private Action action;
	
	@Column(columnDefinition = "text")
	@ColumnType(typeHandler = Map2JsonTypeHandler.class)
	private Map<String, Object> params;
	
	@ColumnType(jdbcType = JdbcType.VARCHAR)
	private ExeStatus exeStatus;
	
	@Column(columnDefinition = "text")
	private String message;
	
	public enum ExeStatus {
		NOT_START, SUCCESS, FAIL,IGNORE
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
