package mt.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义当前用户注解
 * @CurrentUser
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.PARAMETER)
public @interface CurrentUser {

}

/*
 * 在类方法中使用为：@CurrentUser User user
 * 在前台页面 request中获取：${currentUser.username}
 * currentUser：当前用户
 */

/**
 * springmvc 设置拦截器 设置拦截的某些请求可以使用此注解
 * <mvc:interceptors>    
	    <mvc:interceptor>
	    	<mvc:mapping path="/update/**"/>
	    	<mvc:mapping path="/pc/**"/>
	    	<entity class="com.oig.config.argmentResolver.CurrentUserHandlerInterceptor"></entity>
	    </mvc:interceptor>
	</mvc:interceptors> 
 */

