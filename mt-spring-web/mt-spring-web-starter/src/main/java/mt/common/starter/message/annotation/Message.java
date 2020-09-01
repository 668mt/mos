package mt.common.starter.message.annotation;

import mt.common.starter.message.messagehandler.DefaultMessageHandler;
import mt.common.starter.message.messagehandler.MessageHandler;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * @Author Martin
 * @Date 2019/1/6
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Inherited
@Documented
public @interface Message {
	@AliasFor("handlerClass")
	Class<? extends MessageHandler> value() default DefaultMessageHandler.class;
	
	String[] params() default {};
	
	@AliasFor("value")
	Class<? extends MessageHandler> handlerClass() default DefaultMessageHandler.class;
	
	String handlerBeanName() default "";
	
	String condition() default "";
	
	String mark() default "";
}
