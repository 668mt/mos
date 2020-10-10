package mt.spring.mos.server.entity.po;

import mt.spring.mos.server.utils.SizeUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.common.annotation.ForeignKey;
import mt.spring.mos.server.entity.BaseEntity;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.File;

/**
 * @Author Martin
 * @Date 2020/5/18
 */
@Table(name = "mos_resource")
@Data
@EqualsAndHashCode(callSuper = false)
public class Resource extends BaseEntity {
	
	private static final long serialVersionUID = 721502363752246263L;
	
	@Id
	@KeySql(useGeneratedKeys = true)
	private Long id;
	private String pathname;
	private String contentType;
	private Long sizeByte;
	@ForeignKey(tableEntity = Dir.class, casecadeType = ForeignKey.CascadeType.ALL)
	@Column(nullable = false)
	private Long dirId;
	private Boolean isPublic;
	
	@Transient
	public String getReadableSize() {
		return SizeUtils.getReadableSize(sizeByte);
	}
	
	@Transient
	public String getFileName() {
		if (pathname == null) {
			return null;
		}
		return new File(pathname).getName();
	}
}
