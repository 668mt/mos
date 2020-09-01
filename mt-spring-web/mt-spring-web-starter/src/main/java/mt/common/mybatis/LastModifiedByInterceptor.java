package mt.common.mybatis;

import mt.common.annotation.LastModifiedBy;
import mt.common.currentUser.UserContext;
import mt.utils.ReflectUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;

/**
 * 修改日期拦截
 *
 * @author Martin
 * @ClassName: LastModifiedDateInterceptor
 * @Description:
 * @date 2017-9-29 上午10:43:30
 * Mybatis拦截器只能拦截四种类型的接口：Executor、StatementHandler、ParameterHandler和ResultSetHandler。
 */
@Intercepts({
		@Signature(method = "update", type = Executor.class, args = {MappedStatement.class, Object.class})
})
public class LastModifiedByInterceptor implements Interceptor {
	private UserContext userContext;
	
	public LastModifiedByInterceptor(UserContext userContext) {
		this.userContext = userContext;
	}
	
	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
		Object[] args = invocation.getArgs();
		Object parameters = args[1];
		if (parameters == null) {
			return invocation.proceed();
		}
		if (!InterceptorHelper.isSave(ms, parameters) && !InterceptorHelper.isUpdate(ms, parameters)) {
			return invocation.proceed();
		}
		List<Field> fields = ReflectUtils.findAllFields(parameters.getClass(), LastModifiedBy.class);
		for (Field field : fields) {
			field.setAccessible(true);
			field.set(parameters, userContext.getCurrentUserId());
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
