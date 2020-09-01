package mt.common.mybatis.entity;

import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * 列集合
* @ClassName: Columns
* @Description: 
* @author Martin
* @date 2017-10-26 下午6:09:53
*
 */
public class Columns extends ArrayList<Column> {
	private static final long serialVersionUID = -2171332113807342716L;
	

	public boolean add(Column column){
		return super.add(column);
	}
	public boolean add(String column, Object value){
		return super.add(new Column(column, value));
	}
	
	public Object getValue(String column){
		Assert.notNull(column);
		Iterator<Column> iterator = iterator();
		while(iterator.hasNext()){
			Column next = iterator.next();
			if(column.equals(next.getColumnName())){
				return next.getValue();
			}
		}
		return null;
	}
	
	public Column getColumn(String columnName){
		Assert.notNull(columnName);
		Iterator<Column> iterator = iterator();
		while(iterator.hasNext()){
			Column next = iterator.next();
			if(columnName.equals(next.getColumnName())){
				return next;
			}
		}
		return null;
	}
}
