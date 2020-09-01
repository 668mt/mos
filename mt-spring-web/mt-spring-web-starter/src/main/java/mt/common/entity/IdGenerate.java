package mt.common.entity;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * 
* @ClassName: IdGenerate
* @Description: 
* @author Martin
* @date 2017-10-25 下午12:08:46
*
 */
public class IdGenerate implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = -7747749212336225046L;

	@Id
	@Column(name = "tableName")
	private String tableName;

	@Column(name = "nextValue")
    private Long nextValue;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName == null ? null : tableName.trim();
    }

    public Long getNextValue() {
        return nextValue;
    }

    public void setNextValue(Long nextValue) {
        this.nextValue = nextValue;
    }

	public IdGenerate(String tableName, Long nextValue) {
		super();
		this.tableName = tableName;
		this.nextValue = nextValue;
	}

	public IdGenerate() {
		super();
	}
}