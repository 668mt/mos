package mt.common.mybatis;

import mt.common.annotation.Version;
import mt.utils.ReflectUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 版本号拦截
 *
 * @Author LIMAOTAO236
 * @ClassName: LastModifiedDateInterceptor
 * @Description:
 * @date 2017-9-29 上午10:43:30
 * Mybatis拦截器只能拦截四种类型的接口：Executor、StatementHandler、ParameterHandler和ResultSetHandler。
 */
@Intercepts({
		@Signature(method = "update", type = Executor.class, args = {MappedStatement.class, Object.class})
})
@Component
public class VersionInterceptor implements Interceptor {
	
	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
		//获取参数
		Object[] args = invocation.getArgs();
		Object parameters = args[1];
		if (parameters == null) {
			return invocation.proceed();
		}
		if (!InterceptorHelper.isSave(ms, parameters) && !InterceptorHelper.isUpdate(ms, parameters)) {
			return invocation.proceed();
		}
		List<Field> versionFields = ReflectUtils.findAllFields(parameters.getClass(), Version.class);
		//查找版本号注解
		if (versionFields == null || versionFields.size() == 0) {
			return invocation.proceed();
		}
		Map<String, Object> obj = InterceptorHelper.findByPrimaryKey(parameters, invocation);
		for (Field field : versionFields) {
			if (!field.getType().isAssignableFrom(Long.class)) {
				continue;
			}
			field.setAccessible(true);
			
			Long value = null;
			if (obj != null) {
				value = (Long) obj.get(field.getName());
			}
			if (value == null) {
				value = 0L;
			} else {
				value = value + 1;
			}
			field.set(parameters, value);
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
