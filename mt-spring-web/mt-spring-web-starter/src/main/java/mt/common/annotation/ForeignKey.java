package mt.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 外键管理
 *
 * @author Martin
 * @ClassName: ForeightKey
 * @Description:
 * @date 2017-11-30 下午12:02:55
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ForeignKey {
	
	/**
	 * 级联类型
	 *
	 * @author Martin
	 * @ClassName: CascadeType
	 * @Description:
	 * @date 2017-11-30 下午12:07:55
	 */
	public enum CascadeType {
		/**
		 * 所有，包括删除和更新
		 */
		ALL,
		/**
		 * 级联删除
		 */
		DELETE,
		/**
		 * 级联更新
		 */
		UPDATE,
		/**
		 * 脱离，当删除主表数据后，从表把外键设为NULL，需从表外键设置允许为NULL
		 */
		DETACH,
		/**
		 * 默认，不级联
		 */
		DEFAULT
	}
	
	/**
	 * 表名
	 *
	 * @return
	 */
	String table() default "";
	
	Class<?> tableEntity() default Object.class;
	
	/**
	 * 关联列名
	 *
	 * @return
	 */
	String referencedColumnName() default "id";
	
	/**
	 * 级联类型
	 *
	 * @return
	 */
	CascadeType casecadeType() default CascadeType.DEFAULT;
}
