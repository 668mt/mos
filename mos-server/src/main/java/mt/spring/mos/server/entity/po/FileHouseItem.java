package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.common.annotation.ForeignKey;
import mt.generator.mybatis.annotation.Index;
import mt.spring.mos.server.entity.BaseEntity;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @Author Martin
 * @Date 2020/11/21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "mos_file_house_item")
@Index(columns = {"md5", "sizeByte"})
public class FileHouseItem extends BaseEntity {
	@Id
	@KeySql(useGeneratedKeys = true)
	// 主键由数据库生成，不参与 INSERT：PG 下显式插入 NULL 会违反非空约束
	@Column(insertable = false)
	private Long id;
	
	private Integer chunkIndex;
	@Column(nullable = false)
	private Long sizeByte;
	@Column(nullable = false)
	private String md5;
	
	@ForeignKey(tableEntity = FileHouse.class, casecadeType = ForeignKey.CascadeType.ALL)
	private Long fileHouseId;
}
