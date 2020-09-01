package mt.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 创建日期注解
* @ClassName: CreatedDate
* @Description: 
* @author Martin
* @date 2017-9-28 下午6:10:38
*
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface CreatedDate {

}
