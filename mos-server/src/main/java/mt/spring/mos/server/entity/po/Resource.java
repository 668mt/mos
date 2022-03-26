package mt.spring.mos.server.entity.po;

import javafx.scene.input.PickResult;
import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.common.annotation.ForeignKey;
import mt.generator.mybatis.annotation.UniqueIndex;
import mt.spring.mos.base.utils.SizeUtils;
import mt.spring.mos.server.entity.BaseEntity;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;

/**
 * @Author Martin
 * @Date 2020/5/18
 */
@Table(name = "mos_resource")
@Data
@EqualsAndHashCode(callSuper = false)
@UniqueIndex(columns = {"name", "dirId"})
public class Resource extends BaseEntity {
	
	private static final long serialVersionUID = 721502363752246263L;
	
	@Id
	@KeySql(useGeneratedKeys = true)
	private Long id;
	@Column(nullable = false)
	private String name;
	private String contentType;
	@Column(nullable = false)
	private Long sizeByte;
	@ForeignKey(tableEntity = Dir.class, casecadeType = ForeignKey.CascadeType.ALL)
	@Column(nullable = false)
	private Long dirId;
	private Boolean isPublic;
	@ForeignKey(tableEntity = FileHouse.class)
	private Long fileHouseId;
	@ForeignKey(tableEntity = FileHouse.class)
	private Long thumbFileHouseId;
	private String suffix;
	private Integer thumbFails;
	private Long visits;
	private Long lastModified;
	private Boolean isDelete = false;
	private Date deleteTime;
	private Long during;
	private String videoLength;
	
	public Boolean getIsDelete() {
		return isDelete == null ? false : isDelete;
	}
	
	@Transient
	private String md5;
	
	public Integer getThumbFails() {
		return thumbFails == null ? 0 : thumbFails;
	}
	
	public String getContentType() {
		if (contentType != null) {
			return contentType;
		}
		return null;
	}
	
	@Transient
	public String getReadableSize() {
		return SizeUtils.getReadableSize(sizeByte);
	}
	
	@Transient
	public String getExtension() {
		String fileName = getName();
		if (fileName == null) {
			return null;
		}
		int lastIndexOf = fileName.lastIndexOf(".");
		if (lastIndexOf == -1) {
			return null;
		}
		return fileName.substring(lastIndexOf + 1);
	}
	
	public Boolean getIsPublic() {
		return this.isPublic == null ? false : this.isPublic;
	}
}
