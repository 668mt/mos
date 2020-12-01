package mt.spring.mos.base.algorithm.weight;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author Martin
 * @Date 2020/11/29
 */
@Data
@AllArgsConstructor
public class WeightWrapper {
	private int start;
	private int end;
	private WeightAble target;
	
	public boolean isHit(int random) {
		return random >= start && random < end;
	}
}
