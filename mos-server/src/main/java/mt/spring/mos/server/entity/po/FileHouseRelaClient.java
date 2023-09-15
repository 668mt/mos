package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.common.annotation.ForeignKey;
import mt.generator.mybatis.annotation.Index;
import mt.generator.mybatis.annotation.IndexType;
import mt.generator.mybatis.annotation.Indexs;
import mt.spring.mos.server.entity.BaseEntity;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @Author Martin
 * @Date 2020/11/22
 */
@Data
@Table(name = "mos_file_house_rela_client")
@EqualsAndHashCode(callSuper = false)
@Indexs({
	@Index(columns = {"client_id", "file_house_id"}, type = IndexType.unique)
})
public class FileHouseRelaClient extends BaseEntity {
	@Id
	@KeySql(useGeneratedKeys = true)
	private Long id;
	
	@Column(nullable = false)
	private Long clientId;
	
	@ForeignKey(tableEntity = FileHouse.class, casecadeType = ForeignKey.CascadeType.ALL)
	@Column(nullable = false)
	private Long fileHouseId;
	
}
