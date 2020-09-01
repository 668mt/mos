package mt.common.mybatis;

import mt.common.annotation.GenerateClass;
import mt.common.annotation.IdGenerator;
import mt.utils.ReflectUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import javax.persistence.Id;
import javax.persistence.Table;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;

/**
 * ID自动生成
 *
 * @ClassName: LastModifiedDateInterceptor
 * @Description:
 * @Author LIMAOTAO236
 * @date 2017-9-29 上午10:43:30
 * Mybatis拦截器只能拦截四种类型的接口：Executor、StatementHandler、ParameterHandler和ResultSetHandler。
 */
@Intercepts({
		@Signature(method = "update", type = Executor.class, args = {MappedStatement.class, Object.class})
})
@Component
public class IdGenerateInterceptor extends BaseInterceptor {
	
	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		Object parameters = getParameters(invocation);
		if (parameters == null) {
			return invocation.proceed();
		}
		if (!isSave(invocation)) {
			return invocation.proceed();
		}
		List<Field> idFields = ReflectUtils.findAllFields(parameters.getClass(), Id.class);
		Table annotation = AnnotatedElementUtils.getMergedAnnotation(parameters.getClass(), Table.class);
		if (annotation == null) {
			return invocation.proceed();
		}
		String tableName = InterceptorHelper.getTableName(parameters);
		for (Field idField : idFields) {
			idField.setAccessible(true);
			Object value = idField.get(parameters);
			if (value == null) {
				IdGenerator idGenerator = AnnotatedElementUtils.getMergedAnnotation(idField, IdGenerator.class);
				GenerateClass generateClass = AnnotatedElementUtils.getMergedAnnotation(idField, GenerateClass.class);
				if (idGenerator != null) {
					//生成主键
					Object id = InterceptorHelper.generateId(tableName, idGenerator, generateClass, idField.getType(), invocation);
					idField.set(parameters, id);
				}
			}
		}
		
		
		return invocation.proceed();
	}
	
	@Override
	public Object plugin(Object target) {
		return Plugin.wrap(target, this);
	}
	
	@Override
	public void setProperties(Properties properties) {
	}
	
}
