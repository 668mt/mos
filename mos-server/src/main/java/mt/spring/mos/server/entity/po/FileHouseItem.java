package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.common.annotation.ForeignKey;
import mt.spring.mos.server.entity.BaseEntity;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @Author Martin
 * @Date 2020/11/21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "mos_file_house_item")
public class FileHouseItem extends BaseEntity {
	@Id
	@KeySql(useGeneratedKeys = true)
	private Long id;
	
	private Integer chunkIndex;
	private Long sizeByte;
	private String md5;
	
	@ForeignKey(tableEntity = FileHouse.class, casecadeType = ForeignKey.CascadeType.ALL)
	private Long fileHouseId;
}
