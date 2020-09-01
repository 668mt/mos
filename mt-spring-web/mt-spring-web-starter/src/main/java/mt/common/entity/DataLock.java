package mt.common.entity;

import javax.persistence.Column;
import javax.persistence.Id;

/**
 * 数据库锁
* @ClassName: DataLock
* @Description: 
* @author Martin
* @date 2017-11-24 上午9:16:29
*
 */
public class DataLock {

	@Id
	private String id;
	@Column(name = "useKey")
	private String useKey;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getUseKey() {
		return useKey;
	}
	public void setUseKey(String useKey) {
		this.useKey = useKey;
	}
}
