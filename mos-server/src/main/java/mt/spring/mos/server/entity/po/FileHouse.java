package mt.spring.mos.server.entity.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.generator.mybatis.annotation.Index;
import mt.spring.mos.server.entity.BaseEntity;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
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
@Index(columns = {"md5", "sizeByte"})
public class FileHouse extends BaseEntity {
	@Id
	@KeySql(useGeneratedKeys = true)
	private Long id;
	@Column(nullable = false)
	private String md5;
	@Column(nullable = false)
	private String pathname;
	private Integer chunks;
	@Column(nullable = false)
	private Long sizeByte;
	@Column(nullable = false)
	private FileStatus fileStatus;
	private Boolean encode;
	private Integer backFails;
	private Integer dataFragmentsCount;
	
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
