package mt.spring.mos.sdk.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.spring.mos.base.utils.SizeUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author Martin
 * @Date 2020/5/18
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Resource implements Serializable {
	
	private static final long serialVersionUID = 721502363752246263L;
	
	private Date createdDate;
	private String createdBy;
	private Date updatedDate;
	private String updatedBy;
	
	private Long id;
	private String name;
	private String contentType;
	private Long sizeByte;
	private Long dirId;
	private Boolean isPublic;
	private Long fileHouseId;
	private Long thumbFileHouseId;
	private String suffix;
	private Integer thumbFails;
	private Long visits;
	private Long lastModified;
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
	
	public String getReadableSize() {
		return SizeUtils.getReadableSize(sizeByte);
	}
	
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
