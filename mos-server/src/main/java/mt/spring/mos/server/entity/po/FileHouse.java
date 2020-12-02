package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.spring.mos.server.entity.BaseEntity;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * 文件仓库
 *
 * @Author Martin
 * @Date 2020/11/21
 */
@Table(name = "mos_file_house")
@Data
@EqualsAndHashCode(callSuper = false)
public class FileHouse extends BaseEntity {
	@Id
	@KeySql(useGeneratedKeys = true)
	private Long id;
	private String md5;
	private String pathname;
	private Integer chunks;
	private Long sizeByte;
	private FileStatus fileStatus;
	private Boolean encode;
	
	@Transient
	public String getChunkTempPath() {
		return this.pathname + "-parts";
	}
	
	public enum FileStatus {
		/**
		 * 文件状态
		 */
		OK, UPLOADING
	}
}
