package mt.spring.mos.client.entity;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2020/5/18
 */
@Data
public class Resource {
	private String pathname;
	private Long sizeByte;
	
	public String getPathname() {
		return pathname == null ? null : pathname.replace("\\", "/");
	}
}
