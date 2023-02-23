package mt.spring.mos.client.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * @Author Martin
 * @Date 2022/11/13
 */
@Data
public class IsExistsDTO {
	private List<String> pathname;
}
