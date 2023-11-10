package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.generator.mybatis.annotation.Index;
import mt.generator.mybatis.annotation.Indexs;

import javax.persistence.Column;
import javax.persistence.Table;

/**
 * fileHouse删除记录，用来判定文件是否可以删除
 *
 * @Author Martin
 * @Date 2023/10/6
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "mos_file_house_delete_log")
@Indexs({
	@Index(columns = "file_house_id")
})
public class FileHouseDeleteLog extends IdBaseEntity {
	@Column(nullable = false)
	private Long resourceId;
	@Column(nullable = false)
	private Long fileHouseId;
}
