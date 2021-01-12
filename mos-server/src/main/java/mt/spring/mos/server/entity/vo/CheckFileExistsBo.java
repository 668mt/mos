package mt.spring.mos.server.entity.vo;

import lombok.Data;

import java.util.Map;

/**
 * @Author Martin
 * @Date 2021/1/9
 */
@Data
public class CheckFileExistsBo {
	private Map<String, Boolean> checkResults;
}
