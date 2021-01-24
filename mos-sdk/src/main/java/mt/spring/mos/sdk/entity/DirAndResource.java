package mt.spring.mos.sdk.entity;

import lombok.Data;

import java.util.Date;
import java.util.Objects;

/**
 * @Author Martin
 * @Date 2020/9/12
 */
@Data
public class DirAndResource {
	private Boolean isDir;
	private Long id;
	private String path;
	private Long sizeByte;
	private Date createdDate;
	private String createdBy;
	private Date updatedDate;
	private String updatedBy;
	private String icon;
	public Boolean isPublic;
	private String contentType;
	private Long thumbFileHouseId;
	private Long visits;
	private Long lastModified;
	
	private String fileName;
	
	@Override
	public int hashCode() {
		return Objects.hash(isDir, id, path, sizeByte, lastModified);
	}
	
	@Override
	public boolean equals(Object target) {
		if (target == null) {
			return false;
		}
		if (!(target instanceof DirAndResource)) {
			return false;
		}
		DirAndResource targetResource = (DirAndResource) target;
		return Objects.equals(isDir, targetResource.isDir)
				&& Objects.equals(id, targetResource.id)
				&& Objects.equals(path, targetResource.path)
				&& Objects.equals(sizeByte, targetResource.sizeByte)
				&& Objects.equals(lastModified, targetResource.lastModified);
	}
}
