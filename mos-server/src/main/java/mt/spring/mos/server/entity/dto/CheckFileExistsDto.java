package mt.spring.mos.server.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * @Author Martin
 * @Date 2021/1/9
 */
@Data
public class CheckFileExistsDto {
	private List<String> pathnames;
}
