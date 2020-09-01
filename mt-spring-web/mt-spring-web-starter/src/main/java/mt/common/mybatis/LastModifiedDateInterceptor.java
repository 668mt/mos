package mt.common.mybatis;

import mt.common.annotation.LastModifiedDate;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Date;

/**
 * 修改日期拦截
 *
 * @param <T>
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
public class LastModifiedDateInterceptor extends BaseInterceptor {
	
	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		Object parameters = getParameters(invocation);
		if (parameters == null) {
			return invocation.proceed();
		}
		if (!isUpdate(invocation)) {
			return invocation.proceed();
		}
		
		setFieldsValue(parameters, LastModifiedDate.class, true, new InterceptorHelper.AbstractValueGenerator<Date>() {
			@Override
			public Date getValue(Field field) {
				return new Date();
			}
		});
		
		return invocation.proceed();
	}
	
}
