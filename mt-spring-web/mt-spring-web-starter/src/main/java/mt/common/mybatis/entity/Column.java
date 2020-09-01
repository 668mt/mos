package mt.common.mybatis.entity;


import mt.utils.JsonUtils;

import java.io.Serializable;

/**
 * 列
* @ClassName: Column
* @Description: 
* @author Martin
* @date 2017-10-26 下午6:05:43
*
 */
public class Column implements Serializable {

	private static final long serialVersionUID = -1595109700275423919L;
	private String columnName;
	private Object value;
	
	public String getColumnName() {
		return columnName;
	}
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	public Object getValue() {
		return value;
	}
	public void setValue(Object value) {
		if(value instanceof java.util.Map){
			value = JsonUtils.toJson(value);
		}
		this.value = value;
	}
	public Column() {
		super();
	}
	public Column(String columnName, Object value) {
		super();
		this.columnName = columnName;
		setValue(value);
	}
	
}
