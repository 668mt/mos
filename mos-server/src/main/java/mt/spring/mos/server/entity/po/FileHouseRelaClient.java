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
 * @Date 2020/11/22
 */
@Data
@Table(name = "mos_file_house_rela_client")
@EqualsAndHashCode(callSuper = false)
public class FileHouseRelaClient extends BaseEntity {
	@Id
	@KeySql(useGeneratedKeys = true)
	private Long id;
	
	@ForeignKey(tableEntity = Client.class)
	private Long clientId;
	
	@ForeignKey(tableEntity = FileHouse.class, casecadeType = ForeignKey.CascadeType.ALL)
	private Long fileHouseId;
	
}
