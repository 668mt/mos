package mt.spring.mos.server.config.upload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author Martin
 * @Date 2020/6/26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadProcess {
	private String name;
	private double percent;
	private double weight;
	
	public double getWeightPercent() {
		return weight * percent;
	}
	
	public UploadProcess(String name, double weight) {
		this.name = name;
		this.weight = weight;
	}
}
