package mt.common.mybatis.mapper;

import mt.common.mybatis.sqlProvider.BaseSelectProvider;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import tk.mybatis.mapper.annotation.RegisterMapper;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * 自定义通用mapper
 *
 * @param <T> 对象
 * @author Martin
 * @ClassName: BaseMapper
 * @Description:
 * @date 2017-10-9 上午9:54:34
 */
@RegisterMapper
public interface BaseMapper<T> extends Mapper<T> {
	
	/**
	 * 是否存在，对象不为null和不为空的字段会作为条件
	 *
	 * @param record
	 * @return
	 */
	@SelectProvider(type = BaseSelectProvider.class, method = "dynamicSQL")
	boolean exists(T record);
	
	/**
	 * 是否存在
	 *
	 * @param columnName
	 * @param value
	 * @return
	 */
	@SelectProvider(type = BaseSelectProvider.class, method = "dynamicSQL")
	boolean existsKeyValue(@Param("columnName") String columnName, @Param("value") Object value);
	
	/**
	 * 指定列名查找
	 *
	 * @param columnName 列名
	 * @param value      值
	 * @return 返回结果
	 */
	@SelectProvider(type = BaseSelectProvider.class, method = "dynamicSQL")
	T findOne(@Param("columnName") String columnName, @Param("value") Object value);
	
	/**
	 * 指定列名查找
	 *
	 * @param columnName 列名
	 * @param value      值
	 * @return 返回结果列表
	 */
	@SelectProvider(type = BaseSelectProvider.class, method = "dynamicSQL")
	List<T> findList(@Param("columnName") String columnName, @Param("value") Object value);
	
}
