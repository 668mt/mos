package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.common.annotation.ForeignKey;
import mt.spring.mos.server.entity.BaseEntity;
import mt.spring.mos.server.utils.UrlEncodeUtils;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.File;

/**
 * @Author Martin
 * @Date 2020/5/20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "mos_dir")
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
