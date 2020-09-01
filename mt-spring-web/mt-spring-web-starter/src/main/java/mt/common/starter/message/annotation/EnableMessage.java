package mt.common.starter.message.annotation;

import mt.common.starter.message.MessageConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @Author Martin
 * @Date 2019/1/6
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Import({MessageConfiguration.class})
public @interface EnableMessage {
}
