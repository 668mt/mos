package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.common.annotation.ForeignKey;
import mt.generator.mybatis.annotation.Index;
import mt.generator.mybatis.annotation.IndexType;
import mt.spring.mos.server.entity.BaseEntity;
import mt.spring.mos.server.utils.UrlEncodeUtils;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.File;
import java.util.Date;

/**
 * @Author Martin
 * @Date 2020/5/20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "mos_dir")
@Index(columns = {"path", "bucket_id"}, type = IndexType.unique)
public class Dir extends BaseEntity {
	private static final long serialVersionUID = -5233564826534911410L;
	@Id
	@KeySql(useGeneratedKeys = true)
	private Long id;
	@Column(nullable = false)
	private String path;
	@ForeignKey(tableEntity = Dir.class, casecadeType = ForeignKey.CascadeType.ALL)
	private Long parentId;
	@ForeignKey(tableEntity = Bucket.class)
	@Column(nullable = false)
	private Long bucketId;
	@Transient
	private Dir child;
	private Boolean isDelete;
	private Date deleteTime;
	
	public Boolean getIsDelete() {
		return isDelete == null ? false : isDelete;
	}
	
	public String getUrlEncodePath() {
		if (path == null) {
			return null;
		}
		return UrlEncodeUtils.encodePathname(path);
	}
	
	@Transient
	public String getName() {
		if (path == null) {
			return null;
		}
		return "/" + new File(path).getName();
	}
}
