package mt.common.mybatis;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;

import java.lang.annotation.Annotation;
import java.util.Properties;

/**
 * @author limaotao236
 * @date 2019/12/9
 * @email limaotao236@pingan.com.cn
 */
public class BaseInterceptor implements Interceptor {
	protected MappedStatement getMappedStatement(Invocation invocation) {
		return (MappedStatement) invocation.getArgs()[0];
	}
	
	protected Object getParameters(Invocation invocation) {
		//获取参数
		Object[] args = invocation.getArgs();
		return args[1];
	}
	
	protected void setFieldsValue(Object entity, Class<? extends Annotation> annotationClass, boolean force, InterceptorHelper.AbstractValueGenerator<?> abstractValueGenerator) throws IllegalAccessException {
		InterceptorHelper.setFieldsValue(entity, annotationClass, force, abstractValueGenerator);
	}
	
	protected boolean isSave(Invocation invocation) {
		return InterceptorHelper.isSave(getMappedStatement(invocation), getParameters(invocation));
	}
	
	protected boolean isUpdate(Invocation invocation) {
		return InterceptorHelper.isUpdate(getMappedStatement(invocation), getParameters(invocation));
	}
	
	@Override
	public Object intercept(Invocation invocation) throws Throwable {
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
