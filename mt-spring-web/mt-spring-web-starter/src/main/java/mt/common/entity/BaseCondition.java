package mt.common.entity;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询条件
 *
 * @author Martin
 * @ClassName: Condition
 * @Description:
 * @date 2017-11-12 下午4:27:34
 */
public class BaseCondition implements Serializable {
	private static final long serialVersionUID = 43095659935735753L;
	
	@Getter
	private List<String> condition;
	
	public void addCondition(@NotNull String sql) {
		if (condition == null)
			condition = new ArrayList<>();
		condition.add(sql);
	}
	
}
