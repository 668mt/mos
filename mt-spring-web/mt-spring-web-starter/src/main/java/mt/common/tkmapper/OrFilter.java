package mt.common.tkmapper;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author limaotao236
 * @date 2020/4/30
 * @email limaotao236@pingan.com.cn
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class OrFilter extends Filter {
	private Filter[] filters;
	
	public OrFilter(Filter...filters) {
		super();
		this.filters = filters;
	}
}
