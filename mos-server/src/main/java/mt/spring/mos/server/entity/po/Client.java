package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.spring.mos.base.algorithm.weight.WeightAble;
import mt.spring.mos.server.entity.BaseEntity;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

/**
 * @Author Martin
 * @Date 2020/5/16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "mos_client")
public class Client extends BaseEntity implements WeightAble {
	
	private static final long serialVersionUID = -7609365042803611738L;
	@Id
	@KeySql(useGeneratedKeys = true)
	private Long id;
	@Column(unique = true, nullable = false)
	private String name;
	@Column(nullable = false)
	private String ip;
	@Column(nullable = false)
	private Integer port;
	private String remark;
	private Integer weight;
	private Long totalStorageByte;
	private Long usedStorageByte;
	@Column(scale = 3)
	private BigDecimal totalStorageGb;
	@Column(scale = 3)
	private BigDecimal usedStorageGb;
	@Column(scale = 3)
	private BigDecimal usedPercent;
	private ClientStatus status;
	private Date lastBeatTime;
	/**
	 * 保留大小
	 */
	private Long keepSpaceByte;
	@Transient
	private int priority_min;
	@Transient
	private int priority_max;
	
	@Override
	public Integer getWeight() {
		return weight == null ? 50 : weight;
	}
	
	@Transient
	public String getUrl() {
		return "http://" + this.ip + ":" + this.port;
	}
	
	public Long getKeepSpaceByte() {
		return this.keepSpaceByte == null ? 0L : this.keepSpaceByte;
	}
	
	public BigDecimal getUsedPercent() {
		if (usedStorageByte == null || totalStorageByte == null) {
			return null;
		}
		return totalStorageByte == 0L ? BigDecimal.ZERO : BigDecimal.valueOf(usedStorageByte).divide(BigDecimal.valueOf(totalStorageByte), 3, RoundingMode.HALF_UP);
	}
	
	public BigDecimal getTotalStorageGb() {
		if (totalStorageByte == null) {
			return null;
		}
		return BigDecimal.valueOf(totalStorageByte).divide(BigDecimal.valueOf(1024L * 1024 * 1024), 3, RoundingMode.HALF_UP);
	}
	
	public BigDecimal getUsedStorageGb() {
		if (usedStorageByte == null) {
			return null;
		}
		return BigDecimal.valueOf(usedStorageByte).divide(BigDecimal.valueOf(1024L * 1024 * 1024), 3, RoundingMode.HALF_UP);
	}
	
	public enum ClientStatus {
		UP, DOWN, KICKED
	}
}
