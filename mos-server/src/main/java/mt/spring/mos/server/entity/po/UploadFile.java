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
@Table(name = "mos_upload_file")
@EqualsAndHashCode(callSuper = false)
@Indexs({
	@Index(columns = {"bucketId", "path_md5"}, type = IndexType.unique),
})
public class UploadFile extends IdBaseEntity {
	@ForeignKey(tableEntity = Bucket.class, casecadeType = ForeignKey.CascadeType.ALL)
	@Column(nullable = false)
	private Long bucketId;
	/**
	 * 文件路径
	 */
	@Column(nullable = false)
	private String pathMd5;
	@ForeignKey(tableEntity = Client.class, casecadeType = ForeignKey.CascadeType.ALL)
	@Column(nullable = false)
	private Long clientId;
	@Column(nullable = false)
	private String md5;
	@Column(nullable = false, length = 500)
	private String clientPath;
	@Column(nullable = false)
	private Long sizeByte;
	@Column(nullable = false)
	private Integer chunks;
}
