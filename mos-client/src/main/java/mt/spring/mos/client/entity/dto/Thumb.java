package mt.spring.mos.client.entity.dto;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2020/12/10
 */
@Data
public class Thumb {
	private String pathname;
	private Integer width;
	private Integer seconds;
	private Long size;
	private String md5;
	private String format;
}
