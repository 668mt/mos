package mt.common.annotation;

import mt.common.service.IdGenerateService;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ID自动生成注解
 *
 * @author Martin
 * @ClassName: CreatedDate
 * @Description:
 * @date 2017-9-28 下午6:10:38
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface IdGenerator {
	/**
	 * 生成器
	 *
	 * @return
	 */
	IdGenerateService.Generator generator() default IdGenerateService.Generator.IDENTITY;
	
	int maxLength() default 16;
}

