package mt.spring.mos.server.entity.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.spring.mos.server.entity.po.Resource;

/**
 * @Author Martin
 * @Date 2021/2/8
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class GallaryVo extends Resource {
	private String url;
	private String thumbUrl;
}
