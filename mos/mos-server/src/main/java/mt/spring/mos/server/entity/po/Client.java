package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.spring.mos.server.entity.BaseEntity;
import org.springframework.web.client.RestTemplate;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2020/5/16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "mos_client")
public class Client extends BaseEntity {
	
	private static final long serialVersionUID = -7609365042803611738L;
	@Id
	private String clientId;
	private String ip;
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
	@Transient
	private int priority_min;
	@Transient
	private int priority_max;
	
	public Integer getWeight() {
		return weight == null ? 50 : weight;
	}
	
	@Transient
	public String getUrl() {
		return "http://" + this.ip + ":" + this.port;
	}
	
	@SuppressWarnings("rawtypes")
	@Transient
	public Map getInfo(RestTemplate restTemplate) {
		return restTemplate.getForObject("http://" + getIp() + ":" + getPort() + "/client/info", Map.class);
	}
	
	@Transient
	@SuppressWarnings("rawtypes")
	public List getClientResources(RestTemplate restTemplate) {
		try {
			return restTemplate.getForObject("http://" + getIp() + ":" + getPort() + "/client/resources", List.class);
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}
	
	@Transient
	@SuppressWarnings("rawtypes")
	public boolean isEnableImport(RestTemplate restTemplate) {
		Map info = getInfo(restTemplate);
		if (info == null) {
			return false;
		}
		return "true".equals(info.get("isEnableAutoImport") + "");
	}
	
	public BigDecimal getUsedPercent() {
		if (usedStorageByte == null || totalStorageByte == null) {
			return BigDecimal.ZERO;
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
		UP, DOWN
	}
	
}
