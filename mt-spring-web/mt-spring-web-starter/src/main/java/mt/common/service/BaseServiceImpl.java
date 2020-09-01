package mt.common.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.SneakyThrows;
import mt.common.annotation.Filter;
import mt.common.converter.Converter;
import mt.common.entity.BaseCondition;
import mt.common.mybatis.utils.MapperColumnUtils;
import mt.common.mybatis.utils.MyBatisUtils;
import mt.common.starter.message.utils.MessageUtils;
import mt.common.tkmapper.Filter.Operator;
import mt.common.utils.SpringUtils;
import mt.utils.JsUtils;
import mt.utils.MyUtils;
import mt.utils.ReflectUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import tk.mybatis.mapper.common.BaseMapper;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.script.ScriptException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author limaotao236
 */
public abstract class BaseServiceImpl<T> implements BaseService<T> {
	@Override
	public PageInfo<T> findPage(@Nullable Integer pageNum, @Nullable Integer pageSize, @Nullable String orderBy, @Nullable Object condition) {
		if (condition == null) {
			condition = new BaseCondition();
		}
		Class<T> entityClass = getEntityClass();
		List<mt.common.tkmapper.Filter> filters = parseCondition(condition);
		return doPage(pageNum, pageSize, orderBy, () -> getBaseMapper().selectByExample(MyBatisUtils.createExample(entityClass, filters)));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<mt.common.tkmapper.Filter> parseCondition(Object condition) {
		List<mt.common.tkmapper.Filter> filters = new ArrayList<>();
		List<String> condition2 = ReflectUtils.getValue(condition, "condition", List.class);
		if (MyUtils.isNotEmpty(condition2)) {
			for (String sql : condition2) {
				filters.add(new mt.common.tkmapper.Filter(sql, Operator.condition));
			}
		}
		if (condition != null) {
			List<Field> fields = ReflectUtils.findAllFields(condition.getClass(), Filter.class);
			for (Field field : fields) {
				field.setAccessible(true);
				//http参数值
				Object value;
				try {
					value = field.get(condition);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
				if (value instanceof List) {
					if (MyUtils.isEmpty((List<?>) value)) {
						continue;
					}
				}
				if (value instanceof Object[]) {
					if (MyUtils.isEmpty((Object[]) value)) {
						continue;
					}
				}
				if (value != null && !"".equals((value + "").trim())) {
					//获取注解
					Filter annotation = AnnotatedElementUtils.getMergedAnnotation(field, Filter.class);
					Assert.notNull(annotation, "filter注解不能为空");
					String column = StringUtils.isNotBlank(annotation.column()) ? annotation.column() : MapperColumnUtils.parseColumn(field.getName());
					Operator operator = annotation.operator();
					String prefix = annotation.prefix();
					String suffix = annotation.suffix();
					Class<? extends Converter<?>> converterClass = annotation.converter();
					String conditionScript = annotation.condition();
					conditionScript = MessageUtils.replaceVariable(conditionScript, condition, false);
					try {
						Boolean conditionResult = JsUtils.eval(conditionScript);
						if (!conditionResult) {
							continue;
						}
					} catch (ScriptException e) {
						throw new RuntimeException(e);
					}
					
					switch (operator) {
						case condition:
							String sql = annotation.sql();
							if (StringUtils.isNotBlank(sql)) {
								filters.add(new mt.common.tkmapper.Filter(MessageUtils.replaceVariable(sql, condition), operator));
							} else {
								//替换变量
								filters.add(new mt.common.tkmapper.Filter(MessageUtils.replaceVariable(column, condition), operator, value));
							}
							break;
						default:
							Map<String, ? extends Converter<?>> beansOfType = SpringUtils.getBeansOfType(converterClass);
							Converter converter;
							if (beansOfType != null && beansOfType.size() > 0) {
								converter = beansOfType.values().iterator().next();
							} else {
								try {
									converter = converterClass.newInstance();
								} catch (Exception e) {
									throw new RuntimeException(e);
								}
							}
							value = converter.convert(value);
							if (StringUtils.isNotBlank(prefix)) {
								value = prefix + value;
							}
							if (StringUtils.isNotBlank(suffix)) {
								value = value + suffix;
							}
							filters.add(new mt.common.tkmapper.Filter(column, operator, value));
							break;
					}
				}
			}
		}
		return filters;
	}
	
	
	@Override
	public PageInfo<T> doPage(@Nullable Integer pageNum, @Nullable Integer pageSize, @Nullable String orderBy, GetList<T> getList) {
		Class<T> entityClass = getEntityClass();
		//分页
		if (pageNum != null && pageSize != null && pageNum > 0 && pageSize > 0) {
			PageHelper.startPage(pageNum, pageSize);
		}
		//排序
		String realOrderBy = null;
		if (orderBy != null) {
			realOrderBy = orderBy;
		} else {
			List<Field> ids = ReflectUtils.findFields(entityClass, Id.class);
			if (CollectionUtils.isNotEmpty(ids)) {
				List<String> orderBys = new ArrayList<>();
				for (Field id : ids) {
					Column column = id.getAnnotation(Column.class);
					String columnName = id.getName();
					if (column != null && StringUtils.isNotBlank(column.name())) {
						columnName = column.name();
					}
					orderBys.add(MapperColumnUtils.parseColumn(columnName) + " desc");
				}
				realOrderBy = StringUtils.join(orderBys, ",");
			}
		}
		if (StringUtils.isNotBlank(realOrderBy)) {
			PageHelper.orderBy(realOrderBy);
		}
		List<T> rows = getList.getList();
		PageInfo<T> pageInfo = new PageInfo<>(rows);
		pageInfo.setPageNum(pageNum != null ? pageNum : 0);
		pageInfo.setPageSize(pageSize != null ? pageSize : 0);
		pageInfo.setOrderBy(orderBy);
		return pageInfo;
	}
	
	/**
	 * 获取数据库Dao
	 *
	 * @return
	 */
	public abstract mt.common.mybatis.mapper.BaseMapper<T> getBaseMapper();
	
	@Override
	@Transactional(readOnly = true)
	public List<T> findAll() {
		return getBaseMapper().selectAll();
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public int count(List<mt.common.tkmapper.Filter> filters) {
		Assert.notNull(filters);
		return getBaseMapper().selectCountByExample(MyBatisUtils.createExample(getEntityClass(), filters));
	}
	
	@SuppressWarnings("unchecked")
	public Class<T> getEntityClass() {
		return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}
	
	@SuppressWarnings("unchecked")
	public <T2> Class<T2> getEntityClass(Class<? extends BaseMapper<T2>> class1) {
		return (Class<T2>) ((ParameterizedType) class1.getGenericInterfaces()[0]).getActualTypeArguments()[0];
	}
	
	@Override
	public boolean exists(String columnName, Object value) {
		List<T> list = findList(columnName, value);
		return MyUtils.isNotEmpty(list);
	}
	
	@Override
	public boolean existsId(Object record) {
		return getBaseMapper().existsWithPrimaryKey(record);
	}
	
	@Override
	public List<T> findByFilter(mt.common.tkmapper.Filter filter) {
		List<mt.common.tkmapper.Filter> filters = new ArrayList<>();
		filters.add(filter);
		return findByFilters(filters);
	}
	
	@Override
	@Transactional(readOnly = true)
	public T findById(Object record) {
		return getBaseMapper().selectByPrimaryKey(record);
	}
	
	@Override
	@Transactional(readOnly = true)
	public T findOne(String column, Object value) {
		List<mt.common.tkmapper.Filter> filters = new ArrayList<>();
		filters.add(new mt.common.tkmapper.Filter(column, Operator.eq, value));
		return findOneByFilters(filters);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<T> findList(String column, Object value) {
		List<mt.common.tkmapper.Filter> filters = new ArrayList<>();
		filters.add(new mt.common.tkmapper.Filter(column, Operator.eq, value));
		return findByFilters(filters);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<T> findByFilters(List<mt.common.tkmapper.Filter> filters) {
		return getBaseMapper().selectByExample(MyBatisUtils.createExample(getEntityClass(), filters));
	}
	
	@Override
	@Transactional(readOnly = true)
	public T findOneByFilters(List<mt.common.tkmapper.Filter> filters) {
		List<T> findByFilters = findByFilters(filters);
		if (MyUtils.isEmpty(findByFilters)) {
			return null;
		}
		if (findByFilters.size() > 1) {
			throw new RuntimeException("findOneByFilters 查询出多个结果！");
		}
		return findByFilters.get(0);
	}
	
	@Override
	@Transactional(readOnly = true)
	public T findOneByFilter(mt.common.tkmapper.Filter filter) {
		List<mt.common.tkmapper.Filter> filters = new ArrayList<>();
		filters.add(filter);
		return findOneByFilters(filters);
	}
	
	@Override
	@Transactional
	public int save(T record) {
		return getBaseMapper().insert(record);
	}
	
	@Override
	@Transactional
	public int saveList(List<T> records) {
		int update = 0;
		for (T record : records) {
			update += save(record);
		}
		return update;
	}
	
	@Override
	@Transactional
	public int saveSelective(T record) {
		return getBaseMapper().insert(record);
	}
	
	;
	
	@Override
	@Transactional
	public int updateById(T record) {
		return getBaseMapper().updateByPrimaryKey(record);
	}
	
	public String[] getCacheName() {
		return new String[]{};
	}
	
	@Override
	@Transactional
	public int updateByIdSelective(T record) {
		return getBaseMapper().updateByPrimaryKeySelective(record);
	}
	
	@Override
	@Transactional
	public int deleteById(Object record) {
		return getBaseMapper().deleteByPrimaryKey(record);
	}
	
	@Override
	@Transactional
	public int deleteByIds(Object[] records) {
		int update = 0;
		for (Object id : records) {
			update += getBaseMapper().deleteByPrimaryKey(id);
		}
		return update;
	}
	
	@Override
	@Transactional
	public int deleteByFilters(List<mt.common.tkmapper.Filter> filters) {
		return getBaseMapper().deleteByExample(MyBatisUtils.createExample(getEntityClass(), filters));
	}
	
	@Override
	@Transactional
	public int delete(String columnName, String value) {
		List<mt.common.tkmapper.Filter> filters = new ArrayList<>();
		filters.add(new mt.common.tkmapper.Filter(columnName, Operator.eq, value));
		return deleteByFilters(filters);
	}
	
	@Override
	public boolean existsByFilters(List<mt.common.tkmapper.Filter> filters) {
		List<T> byFilters = findByFilters(filters);
		return MyUtils.isNotEmpty(byFilters);
	}
	
	@Override
	public boolean existsByFilter(mt.common.tkmapper.Filter filter) {
		return MyUtils.isNotEmpty(findByFilter(filter));
	}
}
