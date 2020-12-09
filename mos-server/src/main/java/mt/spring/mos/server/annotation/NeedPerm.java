package mt.spring.mos.server.annotation;

import mt.spring.mos.server.entity.BucketPerm;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author Martin
 * @Date 2020/6/3
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NeedPerm {
	@AliasFor("perms")
	BucketPerm[] value() default BucketPerm.SELECT;
	
	@AliasFor("value")
	BucketPerm[] perms() default BucketPerm.SELECT;
}
