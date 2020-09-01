package mt.common.mybatis.exception;

/**
 * 不支持类型
* @ClassName: NotSupportException
* @Description: 
* @author Martin
* @date 2017-10-18 下午3:55:56
*
 */
public class NotSupportException extends MyBatisException{

	private static final long serialVersionUID = -4352388816744884182L;

	public NotSupportException(){
		
	}
	public NotSupportException(String message){
		super(message);
	}
}
