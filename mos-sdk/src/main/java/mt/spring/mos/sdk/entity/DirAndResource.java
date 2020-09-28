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
	private String path;
	private Long sizeByte;
	private String readableSize;
	private String fileName;
	
	private Date createdDate;
	private Date updatedDate;
	private String createdBy;
	private String updatedBy;
	
}
