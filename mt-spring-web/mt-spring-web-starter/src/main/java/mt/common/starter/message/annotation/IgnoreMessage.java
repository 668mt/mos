package mt.common.starter.message.annotation;

import java.lang.annotation.*;

/**
 * @Author Martin
 * @Date 2019/1/6
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
@Documented
public @interface IgnoreMessage {
}
