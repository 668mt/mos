package mt.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段自动生成顺序
* @ClassName: GenerateOrder
* @Description: 
* @author Martin
* @date 2018-4-24 下午2:23:18
*
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD})
public @interface GenerateOrder {
	int value() default 0;
}
