package mt.common.service;


import com.github.pagehelper.PageInfo;
import mt.common.tkmapper.Filter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author limaotao236
 */
public interface BaseService<T> {
	
	public interface GetList<T> {
		List<T> getList();
	}
	
	List<T> findByFilter(Filter filter);
	
	/**
	 * 返回行数
	 *
	 * @param filters
	 * @return
	 */
	int count(List<Filter> filters);
	
	/**
	 * 是否存在主键
	 *
	 * @return
	 */
	boolean existsId(Object record);
	
	int delete(String columnName, String value);
	
	/**
	 * 是否存在
	 *
	 * @param columnName
	 * @param value
	 * @return
	 */
	boolean exists(String columnName, Object value);
	
	List<Filter> parseCondition(Object condition);
	
	PageInfo<T> doPage(@Nullable Integer pageNum, @Nullable Integer pageSize, @Nullable String orderBy, GetList<T> getList);
	
	/**
	 * 查询所有
	 *
	 * @return
	 */
	List<T> findAll();
	
	PageInfo<T> findPage(@Nullable Integer pageNum, @Nullable Integer pageSize, @Nullable String orderBy, @Nullable Object condition);
	
	/**
	 * 通过id查询
	 *
	 * @return 对象
	 */
	T findById(Object record);
	
	/**
	 * 查询一个
	 *
	 * @param column
	 * @param value
	 * @return 对象
	 */
	T findOne(String column, Object value);
	
	/**
	 * 查询列表
	 *
	 * @param filters 过滤
	 * @return 列表
	 */
	List<T> findByFilters(List<Filter> filters);
	
	/**
	 * 查询一个，多个抛出异常
	 *
	 * @param filters
	 * @return 对象
	 */
	T findOneByFilters(List<Filter> filters);
	
	T findOneByFilter(Filter filter);
	
	/**
	 * 查询列表
	 *
	 * @param column 字段名
	 * @param value  值
	 * @return 列表
	 */
	List<T> findList(String column, Object value);
	
	/**
	 * 保存,null值会被保存，不会使用数据库默认值
	 *
	 * @param record 对象
	 * @return 影响条数
	 */
	int save(T record);
	
	int saveList(List<T> records);
	
	/**
	 * 保存,null值不会被保存，会使用数据库默认值
	 *
	 * @param record 对象
	 * @return 影响条数
	 */
	int saveSelective(T record);
	
	/**
	 * 通过主键更新，传入record对象中为null的会被更新
	 *
	 * @param record
	 * @return 影响条数
	 */
	int updateById(T record);
	
	/**
	 * 通过主键更新，传入record对象中为null的不会被更新
	 *
	 * @param record
	 * @return 影响条数
	 */
	int updateByIdSelective(T record);
	
	/**
	 * 通过主键删除
	 *
	 * @return 影响条数
	 */
	int deleteById(Object record);
	
	int deleteByIds(Object[] records);
	
	/**
	 * 删除
	 *
	 * @param filters
	 * @return
	 */
	int deleteByFilters(List<Filter> filters);
	
	boolean existsByFilters(List<Filter> filters);
	
	boolean existsByFilter(Filter filter);
}
