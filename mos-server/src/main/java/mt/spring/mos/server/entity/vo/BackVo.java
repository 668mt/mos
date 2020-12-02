package mt.spring.mos.server.entity.vo;

import lombok.Data;

import java.util.Objects;

/**
 * @Author Martin
 * @Date 2020/10/22
 */
@Data
public class BackVo {
	private Long fileHouseId;
	private Integer dataFragmentsAmount;
	
	@Override
	public int hashCode() {
		return Objects.hash(fileHouseId, dataFragmentsAmount);
	}
	
	@Override
	public boolean equals(Object target) {
		if (target == null) {
			return false;
		}
		if (!target.getClass().equals(getClass())) {
			return false;
		}
		BackVo that = (BackVo) target;
		return Objects.equals(fileHouseId, that.fileHouseId) && Objects.equals(dataFragmentsAmount, that.dataFragmentsAmount);
	}
}
