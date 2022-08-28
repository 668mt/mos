package mt.spring.mos.server.entity.vo;

import lombok.Data;
import mt.spring.mos.server.entity.po.Resource;

import java.util.List;

/**
 * @Author Martin
 * @Date 2022/8/27
 */
@Data
public class DirDetailInfo {
	private List<Resource> thumbs;
	private Long dirCount;
	private Long fileCount;
}
