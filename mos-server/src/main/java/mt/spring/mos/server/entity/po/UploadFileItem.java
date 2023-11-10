package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.common.annotation.ForeignKey;
import mt.generator.mybatis.annotation.Index;
import mt.generator.mybatis.annotation.IndexType;
import mt.generator.mybatis.annotation.Indexs;

import javax.persistence.Column;
import javax.persistence.Table;

/**
 * @Author Martin
 * @Date 2023/9/9
 */
@Data
@Table(name = "mos_upload_file_item")
@EqualsAndHashCode(callSuper = false)
@Indexs({
	@Index(columns = {"upload_file_id", "chunk_index"}, type = IndexType.unique),
})
public class UploadFileItem extends IdBaseEntity {
	@ForeignKey(tableEntity = UploadFile.class, casecadeType = ForeignKey.CascadeType.ALL)
	@Column(nullable = false)
	private Long uploadFileId;
	
	@Column(nullable = false)
	private String md5;
	
	@Column(nullable = false, length = 500)
	private String clientPath;
	
	@Column(nullable = false)
	private Long sizeByte;
	
	@Column(nullable = false)
	private Integer chunkIndex;
}
