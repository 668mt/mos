package mt.spring.mos.sdk.entity;

import lombok.Data;

import java.util.Date;

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
	
}
